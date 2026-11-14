import { useEffect, useRef, useState } from 'react';

const DEFAULT_LEAGUE = 'sunday-league';

export default function App() {
  const [leagueId, setLeagueId] = useState(DEFAULT_LEAGUE);
  const [activeLeague, setActiveLeague] = useState(DEFAULT_LEAGUE);
  const [snapshot, setSnapshot] = useState(null);
  const [status, setStatus] = useState('connecting');
  const [error, setError] = useState('');
  const [flash, setFlash] = useState(false);
  const [eventCount, setEventCount] = useState(0);
  const versionRef = useRef(0);

  useEffect(() => {
    setStatus('connecting');
    setError('');
    setSnapshot(null);
    setEventCount(0);
    versionRef.current = 0;

    const url = `/api/leagues/${encodeURIComponent(activeLeague)}/standings/stream`;
    const source = new EventSource(url);

    source.addEventListener('standings', (event) => {
      try {
        const next = JSON.parse(event.data);
        if (next.version < versionRef.current) {
          return;
        }
        const bumped = next.version > versionRef.current;
        versionRef.current = next.version;
        setSnapshot(next);
        setEventCount((n) => n + 1);
        setStatus('live');
        if (bumped) {
          setFlash(true);
          window.setTimeout(() => setFlash(false), 600);
        }
      } catch (err) {
        setError(`Bad SSE payload: ${err.message}`);
      }
    });

    source.onerror = () => {
      setStatus('reconnecting');
    };

    return () => source.close();
  }, [activeLeague]);

  return (
    <main className={`page ${flash ? 'flash' : ''}`}>
      <header>
        <h1>Live Standings</h1>
        <p className="meta">
          league <strong>{activeLeague}</strong> · status <strong>{status}</strong>
          {snapshot ? ` · version ${snapshot.version}` : ''}
          {` · sse events ${eventCount}`}
          {snapshot?.updatedAt ? ` · updated ${snapshot.updatedAt}` : ''}
        </p>
        <p className="hint">
          Watch <code>sunday-league</code> (default). Version and “sse events” should tick when results apply.
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const next = leagueId.trim() || DEFAULT_LEAGUE;
            setActiveLeague(next);
          }}
        >
          <input
            value={leagueId}
            onChange={(e) => setLeagueId(e.target.value)}
            placeholder="league id"
            aria-label="League id"
          />
          <button type="submit">Watch</button>
        </form>
      </header>

      {error ? <p className="error">{error}</p> : null}

      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Team</th>
            <th>P</th>
            <th>W</th>
            <th>D</th>
            <th>L</th>
            <th>GF</th>
            <th>GA</th>
            <th>GD</th>
            <th>Pts</th>
          </tr>
        </thead>
        <tbody>
          {(snapshot?.rows ?? []).map((row) => (
            <tr key={row.team}>
              <td>{row.position}</td>
              <td>{row.team}</td>
              <td>{row.played}</td>
              <td>{row.won}</td>
              <td>{row.drawn}</td>
              <td>{row.lost}</td>
              <td>{row.goalsFor}</td>
              <td>{row.goalsAgainst}</td>
              <td>{row.goalDiff}</td>
              <td>{row.points}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {!snapshot?.rows?.length ? <p className="empty">No rows yet — submit results to this league.</p> : null}
    </main>
  );
}
