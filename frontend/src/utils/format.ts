export const label = (s: string) =>
  s
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/(^|\s)\S/g, (s) => s.toUpperCase());
export const date = (s: string | null | undefined) =>
  s
    ? new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(s))
    : "—";
export const shortDate = (s: string) =>
  new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" }).format(
    new Date(s),
  );
export const initials = (s: string) =>
  s
    .split(" ")
    .map((x) => x[0])
    .slice(0, 2)
    .join("");
export function remaining(deadline: string, done = false) {
  if (done) return "Completed";
  const minutes = Math.ceil(
    (new Date(deadline).getTime() - Date.now()) / 60000,
  );
  if (minutes < 0)
    return `${Math.abs(minutes) < 60 ? Math.abs(minutes) + "m" : Math.round(Math.abs(minutes) / 60) + "h"} overdue`;
  return minutes < 60
    ? `${minutes}m left`
    : `${Math.floor(minutes / 60)}h ${minutes % 60}m left`;
}
