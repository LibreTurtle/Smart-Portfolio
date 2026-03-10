package com.fierceadventurer.smartportfoliobackend.payment.service;

import com.fierceadventurer.smartportfoliobackend.payment.dto.SponsorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SponsorQueryService {
    private final JdbcClient jdbcClient;

    public List<SponsorResponse> getRecentSponsors(){
        String sql = """
            SELECT sponsor_name AS sponsorName, amount, currency 
            FROM sponsors 
            WHERE status = 'SUCCESS' 
            ORDER BY created_at DESC 
            LIMIT 10
        """;

        return jdbcClient.sql(sql).query(SponsorResponse.class)
                .list();
    }
}
