import { get } from "../services/api";
import type { Incident } from "../types";

function csvCell(text: string) {
  const safe = /^[\s]*[=+@-]|^[\t\r\n]/.test(text) ? "'" + text : text;
  return '"' + safe.replaceAll('"', '""') + '"';
}
export async function exportIncidents(format: "csv" | "xlsx", filters: URLSearchParams) {
  const query = new URLSearchParams(filters);
  query.delete("page"); query.delete("size");
  const incidents = await get<Incident[]>(`/incidents/export?${query}`);
  const headers = ["Incident ID", "Title", "Affected service", "Category", "Created (UTC)", "Priority", "Status", "Assigned technician", "SLA status", "Resolution deadline (UTC)"];
  const rows = incidents.map(i => [`INC-${i.id}`, i.title, i.service.name, i.category.name, i.createdAt,
    i.priority, i.status, i.technician?.name ?? "Unassigned", i.slaLevel, i.resolutionDeadline]);
  let file: Blob;
  if (format === "xlsx") {
    const { Workbook } = await import("exceljs");
    const workbook = new Workbook();
    const sheet = workbook.addWorksheet("Incidents", { views: [{ state: "frozen", ySplit: 1 }] });
    sheet.addRows([headers, ...rows]);
    sheet.columns.forEach((column, index) => { column.width = index === 1 ? 52 : 25; });
    sheet.getRow(1).font = { bold: true };
    sheet.autoFilter = "A1:J1";
    file = new Blob([await workbook.xlsx.writeBuffer()], {type:"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
  } else {
    file = new Blob(["\uFEFF", [headers,...rows].map(row=>row.map(csvCell).join(",")).join("\r\n")], {type:"text/csv;charset=utf-8"});
  }
  const url = URL.createObjectURL(file);
  const link = document.createElement("a");
  link.href = url; link.download = "incidents." + format; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
