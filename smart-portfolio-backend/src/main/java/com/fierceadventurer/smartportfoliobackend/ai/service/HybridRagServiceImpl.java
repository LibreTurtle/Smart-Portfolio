package com.fierceadventurer.smartportfoliobackend.ai.service;

import com.fierceadventurer.smartportfoliobackend.ai.dto.ChatRequest;
import com.fierceadventurer.smartportfoliobackend.ai.repository.SemanticCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class HybridRagServiceImpl implements HybridRagService{

    private final SemanticCacheRepository cacheRepository;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;

    public HybridRagServiceImpl(
            SemanticCacheRepository cacheRepository,
            VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            ChatClient.Builder chatClientBuilder) {
        this.cacheRepository = cacheRepository;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Flux<String> streamQuestion(ChatRequest request) {
        String question = request.question();
        log.info("Recieved AI question for streaming: {}", question);

        //Embedding
        float[] floatEmbedding = embeddingModel.embed(question);
        List<Double> doubleEmbedding = IntStream.range(0 , floatEmbedding.length)
                .mapToDouble(i -> floatEmbedding[i])
                .boxed()
                .toList();

        //Semantic Cache check
        var cacheAnswer = cacheRepository.findCacheResponse(doubleEmbedding);
        if(cacheAnswer.isPresent()){
            log.info("Semantic cache HIT! Streaming instant response.");
            return Flux.just(cacheAnswer.get());
        }

        log.info("Semantic cache MISS. Querying pgvector and streaming from Groq...");

        //RAG Retrieval
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(3).build()
        );

        String context = similarDocuments.stream()
                .map(Document::getText).
                collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                You are the personal AI assistant for Ayush Tripathi's professional portfolio.
                Use ONLY the following context about his skills, projects, and experience to answer the visitor's question.
                If the answer is not in the context, politely state that you don't have that specific information and encourage them to use the contact form.
                
                CONTEXT:
                {context}
                """;

        StringBuilder fullResponseBuilder = new StringBuilder();

        return chatClient.prompt()
                .system(s -> s.text(systemPrompt).param("context", context))
                .user(question)
                .stream()
                .content()
                .doOnNext(fullResponseBuilder::append)
                .doOnComplete(()->{
                    log.info("Stream complete. Saving full response to cache.");
                    cacheRepository.saveToCache(question, doubleEmbedding, fullResponseBuilder.toString());
                });
    }
}
