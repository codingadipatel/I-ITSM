import { useState } from "react";
import { useApi } from "../hooks/useApi";
import { Loading, ErrorState } from "./UI";

interface Counts { previous: number; selected: number }
interface Comparison {
  selectedStart: string; previousStart: string; endExclusive: string; total: Counts;
  technicians: { id: number | null; name: string; counts: Counts }[];
}
function inclusiveEnd(exclusive: string) {
  const end = new Date(exclusive + "T00:00:00Z");
  end.setUTCDate(end.getUTCDate() - 1);
  return end.toISOString().slice(0,10);
}
export function IncidentComparison() {
  const [period, setPeriod] = useState("month");
  const [anchor, setAnchor] = useState(new Date().toISOString().slice(0,10));
  const { data, loading, error } = useApi<Comparison>(anchor ? `/reports/comparison?period=${period}&date=${anchor}` : null);
  return <section className="panel">
    <div className="panel-header"><div><h2>Incident comparisons</h2>
      <p>Incidents by creation date (UTC), grouped by current assigned technician. The current period includes reports received so far.</p></div></div>
    <div className="panel-body form-grid"><label>Compare by<select value={period} onChange={e=>setPeriod(e.target.value)}>
      <option value="week">Week (Monday–Sunday)</option><option value="month">Month</option><option value="year">Year</option>
    </select></label><label>Date in selected period<input type="date" value={anchor} onChange={e=>setAnchor(e.target.value)}/></label></div>
    {!anchor ? <p>Select a date.</p> : loading ? <Loading/> : error ? <ErrorState message={error}/> : data &&
      <div className="table-wrap"><table><thead><tr><th>Assigned technician</th>
        <th>{data.previousStart} – {inclusiveEnd(data.selectedStart)}</th>
        <th>{data.selectedStart} – {inclusiveEnd(data.endExclusive)}</th><th>Change</th></tr></thead>
        <tbody>{[{ id: "total", name: "All visible incidents", counts: data.total }, ...data.technicians].map(row => {
          const change = row.counts.selected - row.counts.previous;
          return <tr key={row.id ?? "unassigned"}><th scope="row">{row.name}</th><td>{row.counts.previous}</td>
            <td>{row.counts.selected}</td><td>{change > 0 ? "+" : ""}{change}</td></tr>;
        })}</tbody></table></div>}
  </section>;
}
