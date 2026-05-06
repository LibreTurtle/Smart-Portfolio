import type {
  APIResponse,
  Project,
  Profile,
  Education,
  Experience,
  Certification,
  Achievement,
  Skill,
  WorkHighlights,
  ContactRequest,
  ContactMessageResponse,
  ChatResponse,
} from "./types";
import { apiUrl } from "./public-api";

async function fetchAPI<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const res = await fetch(apiUrl(path), {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  const envelope: APIResponse<T> = await res.json();
  if (!envelope.success) {
    throw new Error(envelope.error?.message || "Unknown error");
  }
  return envelope.data as T;
}

export const getProjects = () => fetchAPI<Project[]>("/api/projects");
export const getWorkHighlights = () =>
  fetchAPI<WorkHighlights>("/api/projects/highlights");

export const getProject = (id: string) =>
  fetchAPI<Project>(`/api/projects/${id}`);

export const getProfile = () => fetchAPI<Profile>("/api/profile");
export const getEducation = () => fetchAPI<Education[]>("/api/profile/education");
export const getExperience = () => fetchAPI<Experience[]>("/api/profile/experience");
export const getCertifications = () => fetchAPI<Certification[]>("/api/profile/certifications");
export const getAchievements = () => fetchAPI<Achievement[]>("/api/profile/achievements");
export const getSkills = () => fetchAPI<Skill[]>("/api/profile/skills");

export const submitContact = (data: ContactRequest) =>
  fetchAPI<ContactMessageResponse>("/api/contact", {
    method: "POST",
    body: JSON.stringify(data),
  });

export const askChat = (question: string) =>
  fetchAPI<ChatResponse>("/api/chat", {
    method: "POST",
    body: JSON.stringify({ question }),
  });
