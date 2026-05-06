import { For, createSignal, onMount } from "solid-js";
import { getAchievements } from "../../lib/api";

export interface AchievementItem {
  title: string;
  metric: string;
  description: string;
}

interface AchievementsGridProps {
  items: AchievementItem[];
}

export default function AchievementsGrid(props: AchievementsGridProps) {
  const [items, setItems] = createSignal(props.items);

  onMount(() => {
    void loadAchievements();
  });

  async function loadAchievements(): Promise<void> {
    try {
      const data = await getAchievements();
      if (!data || data.length === 0) return;

      const normalized = data
        .slice(0, 6)
        .map((item) => ({
          title: String(item.title).toUpperCase().replace(/\s+/g, "_"),
          metric: String(item.metric || "KEY_SIGNAL").toUpperCase().replace(/\s+/g, "_"),
          description: String(item.description).trim(),
        }));

      setItems(normalized);
    } catch {
      return;
    }
  }


  return (
    <div id="achievement-grid" class="app-scrollbar grid grid-cols-1 md:grid-cols-2 gap-px bg-border border border-border max-h-[22rem] overflow-y-auto">
      <For each={items()}>
        {(item) => (
          <article class="bg-background p-6 md:p-7 flex flex-col justify-between min-h-56 space-y-8 min-w-0">
            <div class="space-y-4">
              <span class="block text-[9px] font-mono text-brand-orange uppercase tracking-[0.14em] [overflow-wrap:anywhere]">{item.metric}</span>
              <h4 class="text-base font-mono font-bold tracking-tight text-foreground [overflow-wrap:anywhere]">{item.title}</h4>
              <p class="text-[11px] leading-relaxed text-muted-foreground font-mono tracking-tight">{item.description}</p>
            </div>
            <div class="pt-4 border-t border-border">
              <span class="block text-[9px] font-mono text-brand-green uppercase tracking-[0.14em] [overflow-wrap:anywhere]">STATUS_CONFIRMED</span>
            </div>
          </article>
        )}
      </For>
    </div>
  );
}
