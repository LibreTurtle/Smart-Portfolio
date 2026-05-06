import { For, Show, createSignal, onCleanup, onMount } from "solid-js";
import { getWorkHighlights } from "../../lib/api";
import type { WorkHighlights } from "../../lib/types";

declare global {
  interface Window {
    refreshCustomScrollbars?: () => void;
  }
}

export default function ProjectsGrid() {
  const [works, setWorks] = createSignal<WorkHighlights | null>(null);
  const [error, setError] = createSignal(false);
  let sliderRef: HTMLDivElement | undefined;

  onMount(() => {
    publishProjectsStatus("fetching");
    if (sliderRef) {
      attachMouseDragScroll(sliderRef);
    }
    refreshProjectsScrollbar();
    void loadProjects();
  });

  async function loadProjects(): Promise<void> {
    try {
      const data = await getWorkHighlights();
      setWorks({
        items: orderWorkItems(Array.isArray(data.items) ? data.items : []),
        github: data.github,
      });
      publishProjectsStatus("ready");
      refreshProjectsScrollbar();
    } catch {
      setError(true);
      publishProjectsStatus("offline");
      refreshProjectsScrollbar();
    }
  }

  return (
    <div
      id="projects-grid"
      ref={sliderRef}
      class="app-scrollbar flex overflow-x-auto snap-x snap-mandatory gap-px border border-border bg-border"
    >
      <Show when={error()}>
        <div class="w-full bg-background p-12 text-center text-brand-orange font-mono text-xs uppercase tracking-widest">
          CRITICAL_ERROR: REMOTE_REGISTRY_OFFLINE
        </div>
      </Show>

      <Show when={!error() && works() === null}>
        <For each={[1, 2, 3, 4, 5, 6]}>
          {() => (
            <div class="min-w-[18rem] md:min-w-[22rem] h-[20rem] md:h-[22rem] snap-start shrink-0 bg-background p-6 animate-pulse flex flex-col justify-between">
              <div class="space-y-4">
                <div class="h-4 bg-muted w-2/3" />
                <div class="h-12 bg-muted w-full" />
              </div>
              <div class="h-4 bg-muted w-1/2" />
            </div>
          )}
        </For>
      </Show>

      <Show when={!error() && works() !== null && works()!.items.length === 0}>
        <div class="w-full bg-background p-12 text-center text-muted-foreground font-mono text-xs uppercase tracking-widest">
          No matching registry entries found.
        </div>
      </Show>

      <For each={works()?.items ?? []}>
        {(project) => (
          <article class="min-w-[18rem] md:min-w-[22rem] max-w-[22rem] h-[20rem] md:h-[22rem] snap-start shrink-0 bg-background p-6 hover:bg-muted/50 transition-all flex flex-col justify-between group">
            <div class="space-y-4">
              <div class="flex justify-between items-start">
                <div class="space-y-2">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="text-[9px] font-mono text-muted-foreground uppercase tracking-[0.24em]">
                      {project.source === "github" ? "GITHUB" : "RESUME"}
                    </span>
                    <Show when={project.is_pinned}>
                      <span class="text-[9px] font-mono text-brand-orange uppercase tracking-[0.24em]">
                        PINNED
                      </span>
                    </Show>
                  </div>
                  <h4 class="font-mono text-lg font-bold tracking-tight text-foreground group-hover:text-brand-green transition-colors">
                  {project.title.toUpperCase()}
                  </h4>
                </div>
                <div class="flex gap-4 text-muted-foreground">
                  <Show when={project.github_url}>
                    <a href={project.github_url} target="_blank" rel="noreferrer" class="transition-colors hover:text-brand-orange dark:hover:text-brand-green">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
                        <path d="M9 18c-4.51 2-5-2-7-2" />
                      </svg>
                    </a>
                  </Show>
                </div>
              </div>
              <p class="text-[11px] leading-relaxed text-muted-foreground font-mono tracking-tight line-clamp-6">
                {project.description}
              </p>
            </div>

            <div class="space-y-4">
              <Show when={project.tech_stack}>
                <div class="flex flex-wrap gap-2">
                  <For each={project.tech_stack?.split(",") ?? []}>
                    {(tech) => (
                      <span class="text-[9px] font-mono text-muted-foreground uppercase border border-border px-1.5 py-0.5 tracking-tighter">
                        {tech.trim()}
                      </span>
                    )}
                  </For>
                </div>
              </Show>
              <div class="pt-4 border-t border-border flex justify-between items-center">
                <span class="text-[9px] font-mono text-zinc-500 uppercase tracking-widest">
                  {project.source === "github" && project.stars
                    ? `STARS_${project.stars}`
                    : "V.0.1_STABLE"}
                </span>
                <Show when={project.live_url}>
                  <a href={project.live_url} target="_blank" class="text-[9px] font-mono text-brand-green hover:underline uppercase tracking-widest">
                    DEPLOYMENT_LINK
                  </a>
                </Show>
              </div>
            </div>
          </article>
        )}
      </For>

      <Show when={works()?.github?.repositories_url}>
        <div class="min-w-[18rem] md:min-w-[22rem] max-w-[22rem] h-[20rem] md:h-[22rem] snap-start shrink-0 bg-background p-6 transition-all hover:bg-muted/50">
          <a
            href={works()!.github!.repositories_url}
            target="_blank"
            rel="noreferrer"
            class="group relative flex h-full flex-col justify-between overflow-hidden border border-border bg-background px-5 py-5 transition-colors hover:border-brand-orange dark:hover:border-brand-green"
          >
            <div class="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,transparent_0%,transparent_62%,color-mix(in_srgb,var(--brand-orange,#ff8c00)_16%,transparent)_62%,color-mix(in_srgb,var(--brand-orange,#ff8c00)_16%,transparent)_64%,transparent_64%)] opacity-80"></div>
            <div class="space-y-4">
              <div class="flex items-center justify-between gap-3">
                <div class="text-[9px] font-mono uppercase tracking-[0.24em] text-muted-foreground">
                  EXIT_NODE
                </div>
                <div class="text-[9px] font-mono uppercase tracking-[0.24em] text-brand-green dark:text-brand-orange">
                  GITHUB.COM
                </div>
              </div>
              <div class="space-y-2">
                <h4 class="font-mono text-lg font-bold tracking-tight text-foreground transition-colors group-hover:text-brand-green">
                  VIEW_MORE_WORKS
                </h4>
                <p class="text-[11px] leading-relaxed font-mono tracking-tight text-muted-foreground">
                  Continue to the full GitHub repository index to inspect the complete public archive and activity feed.
                </p>
              </div>
            </div>
            <div class="flex items-center justify-between border-t border-border pt-4">
              <span class="text-[9px] font-mono uppercase tracking-widest text-zinc-500">
                END_OF_STREAM
              </span>
              <span class="text-[9px] font-mono uppercase tracking-widest text-brand-green dark:text-brand-orange">
                OPEN_EXTERNAL
              </span>
            </div>
          </a>
        </div>
      </Show>
    </div>
  );
}

function orderWorkItems(items: WorkHighlights["items"]): WorkHighlights["items"] {
  return [...items].sort((a, b) => {
    if (a.source === b.source) return 0;
    return a.source === "resume" ? -1 : 1;
  });
}

function attachMouseDragScroll(element: HTMLDivElement): void {
  let isDragging = false;
  let startX = 0;
  let startScrollLeft = 0;
  let dragDistance = 0;
  let dragBlocked = false;

  const onPointerDown = (event: PointerEvent) => {
    if (event.button !== 0) return;
    if ((event.target as HTMLElement | null)?.closest("a, button, input, textarea, select, summary")) {
      dragBlocked = true;
      return;
    }

    dragBlocked = false;
    isDragging = true;
    dragDistance = 0;
    startX = event.clientX;
    startScrollLeft = element.scrollLeft;
    element.setPointerCapture(event.pointerId);
    element.classList.add("is-pointer-dragging");
  };

  const onPointerMove = (event: PointerEvent) => {
    if (!isDragging || dragBlocked) return;

    const delta = event.clientX - startX;
    dragDistance = Math.max(dragDistance, Math.abs(delta));
    element.scrollLeft = startScrollLeft - delta;
  };

  const endDrag = (event?: PointerEvent) => {
    if (!isDragging) {
      dragBlocked = false;
      return;
    }

    isDragging = false;
    dragBlocked = false;
    if (event && element.hasPointerCapture(event.pointerId)) {
      element.releasePointerCapture(event.pointerId);
    }
    element.classList.remove("is-pointer-dragging");
  };

  const onClick = (event: MouseEvent) => {
    if (dragDistance > 6 && !(event.target as HTMLElement | null)?.closest("a")) {
      event.preventDefault();
      event.stopPropagation();
    }
  };

  element.addEventListener("pointerdown", onPointerDown);
  element.addEventListener("pointermove", onPointerMove);
  element.addEventListener("pointerup", endDrag);
  element.addEventListener("pointercancel", endDrag);
  element.addEventListener("click", onClick, true);

  onCleanup(() => {
    element.removeEventListener("pointerdown", onPointerDown);
    element.removeEventListener("pointermove", onPointerMove);
    element.removeEventListener("pointerup", endDrag);
    element.removeEventListener("pointercancel", endDrag);
    element.removeEventListener("click", onClick, true);
  });
}

function refreshProjectsScrollbar(): void {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const scrollbar = document.querySelector<HTMLElement>('[data-scrollbar-for="projects-grid"]');
      if (scrollbar) {
        delete scrollbar.dataset.scrollbarAttached;
      }
      window.refreshCustomScrollbars?.();
    });
  });
}

function publishProjectsStatus(status: "fetching" | "ready" | "offline"): void {
  window.dispatchEvent(
    new CustomEvent("projects-status-change", {
      detail: { status },
    }),
  );
}
