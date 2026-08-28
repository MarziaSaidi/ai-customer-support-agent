export type Theme = "light" | "dark" | "system";

export const THEME_STORAGE_KEY = "supportiq-theme";

/**
 * Runs before first paint in the document head so the page never flashes the
 * wrong theme. Kept in sync with `applyTheme` below.
 */
export const themeInitScript = `
(function() {
  try {
    var stored = localStorage.getItem('${THEME_STORAGE_KEY}');
    var theme = stored === 'light' || stored === 'dark' ? stored : 'system';
    var dark = theme === 'dark' ||
      (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.classList.toggle('dark', dark);
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
  } catch (e) {}
})();
`.trim();

const listeners = new Set<() => void>();
let mediaBound = false;

function emit() {
  for (const listener of listeners) listener();
}

function prefersDark() {
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

function readTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return stored === "light" || stored === "dark" ? stored : "system";
  } catch {
    return "system";
  }
}

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  const dark = theme === "dark" || (theme === "system" && prefersDark());
  root.classList.toggle("dark", dark);
  root.style.colorScheme = dark ? "dark" : "light";
}

/**
 * The theme lives in localStorage and on <html>, both outside React — so it is
 * read through useSyncExternalStore rather than mirrored into state.
 */
export function subscribeToTheme(listener: () => void) {
  listeners.add(listener);

  if (!mediaBound) {
    mediaBound = true;
    // While the user is on "system", follow the OS.
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
      if (readTheme() === "system") {
        applyTheme("system");
        emit();
      }
    });
    // Keep other tabs in step.
    window.addEventListener("storage", (event) => {
      if (event.key === THEME_STORAGE_KEY) {
        applyTheme(readTheme());
        emit();
      }
    });
  }

  return () => {
    listeners.delete(listener);
  };
}

export function getThemeSnapshot(): Theme {
  return readTheme();
}

export function getResolvedThemeSnapshot(): "light" | "dark" {
  return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

/** The server has no way to know the preference; "system" is the safe default. */
export function getServerThemeSnapshot(): Theme {
  return "system";
}

export function getServerResolvedThemeSnapshot(): "light" | "dark" {
  return "light";
}

export function setTheme(next: Theme) {
  const root = document.documentElement;
  root.classList.add("theme-switching");

  applyTheme(next);

  try {
    if (next === "system") {
      localStorage.removeItem(THEME_STORAGE_KEY);
    } else {
      localStorage.setItem(THEME_STORAGE_KEY, next);
    }
  } catch {
    /* Preference just won't persist; the current page still switches. */
  }

  emit();
  window.setTimeout(() => root.classList.remove("theme-switching"), 300);
}
