import { For, createSignal, onMount } from "solid-js";
import { getExperience } from "../../lib/api";

export interface ExperienceItem {
  company: string;
  role: string;
  period: string;
  summary: string;
  stack?: string[];
}

interface ExperienceListProps {
  items: ExperienceItem[];
}

export default function ExperienceList(props: ExperienceListProps) {
  const [items, setItems] = createSignal(props.items);

  onMount(() => {
    void loadExperience();
  });

  async function loadExperience(): Promise<void> {
    try {
      const data = await getExperience();
      if (!data || data.length === 0) return;

      const normalized = data
        .slice(0, 4)
        .map((item) => ({
          company: String(item.company).toUpperCase().replace(/\s+/g, "_"),
          role: String(item.role).toUpperCase().replace(/\s+/g, "_"),
          period: `${item.start_date} - ${item.end_date}`.toUpperCase().replace(/\s+/g, "_"),
          summary: item.summary.trim(),
          stack: item.tech_stack 
            ? item.tech_stack.split(',').map(s => s.trim().toUpperCase().replace(/\s+/g, "_"))
            : [],
        }));

      setItems(normalized);
    } catch {
      return;
    }
  }

  return (
    <div id="experience-list" class="app-scrollbar grid grid-cols-1 xl:grid-cols-2 gap-px bg-border border border-border max-h-[62svh] overflow-y-auto">
      <For each={items()}>
        {(item) => (
          <article class="bg-card p-6 md:p-8 space-y-5">
            <div class="flex items-start justify-between gap-6">
              <div class="space-y-2">
                <span class="text-[9px] font-mono text-muted-foreground uppercase tracking-[0.2em]">NODE</span>
                <h4 class="text-lg font-mono font-bold tracking-tight text-foreground">{item.company}</h4>
              </div>
              <span class="text-[9px] font-mono text-brand-orange uppercase tracking-[0.2em] whitespace-nowrap">{item.period}</span>
            </div>

            <div class="border-l border-border pl-4 space-y-3">
              <p class="text-[10px] font-mono text-brand-green uppercase tracking-[0.2em]">{item.role}</p>
              <p class="text-[11px] leading-relaxed text-muted-foreground font-mono tracking-tight">{item.summary}</p>
            </div>

            <div class="flex flex-wrap gap-2">
              <For each={item.stack ?? []}>
                {(tech) => (
                  <span class="text-[9px] font-mono text-muted-foreground uppercase border border-border px-2 py-1 tracking-widest">
                    {tech}
                  </span>
                )}
              </For>
            </div>
          </article>
        )}
      </For>
    </div>
  );
}
