export interface APIResponse<T> {
  success: boolean;
  data?: T;
  error?: { code: number; message: string };
}

export interface Project {
  id: string;
  title: string;
  description: string;
  tech_stack?: string;
  github_url?: string;
  live_url?: string;
  created_at: string;
}

export interface WorkItem {
  id: string;
  title: string;
  description: string;
  tech_stack?: string;
  github_url?: string;
  live_url?: string;
  source: "github" | "resume";
  stars?: number;
  is_pinned?: boolean;
  updated_at?: string;
  created_at: string;
}

export interface Profile {
  id: string;
  first_name: string;
  last_name: string;
  primary_role: string;
  specialization: string;
  location: string;
  summary: string;
}

export interface Education {
  id: string;
  institution: string;
  degree: string;
  location: string;
  start_date: string;
  end_date: string;
  gpa: string;
  coursework: string;
}

export interface Experience {
  id: string;
  company: string;
  role: string;
  location: string;
  start_date: string;
  end_date: string;
  summary: string;
  tech_stack: string;
}

export interface Certification {
  id: string;
  name: string;
  issuer: string;
  issue_date: string;
  url: string;
}

export interface Achievement {
  id: string;
  title: string;
  metric: string;
  description: string;
  date: string;
}

export interface Skill {
  id: string;
  category: string;
  name: string;
}

export interface GitHubProfile {
  username: string;
  display_name?: string;
  profile_url: string;
  repositories_url: string;
  avatar_url?: string;
}

export interface WorkHighlights {
  items: WorkItem[];
  github?: GitHubProfile;
}

export interface ContactRequest {
  sender_name: string;
  sender_email: string;
  message_body: string;
}

export interface ContactMessageResponse {
  id: string;
  sender_name: string;
  submitted_at: string;
}

export interface ChatResponse {
  answer: string;
  cached: boolean;
}
