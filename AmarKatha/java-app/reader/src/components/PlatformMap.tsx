import type { PlatformRoute } from '../types';

interface PlatformMapProps {
  routes: PlatformRoute[];
}

const statusLabels: Record<PlatformRoute['status'], string> = {
  live: 'Live',
  preview: 'Preview',
  planned: 'Planned',
};

function resolveHref(path: string): string {
  if (path.includes('{')) {
    return path.replace('{slug}', 'monsoon-diaries').replace('{id}', '1').replace('{chapter}', 'chapter-1');
  }
  return path;
}

export default function PlatformMap({ routes }: PlatformMapProps) {
  const grouped = routes.reduce<Record<string, PlatformRoute[]>>((acc, route) => {
    acc[route.area] = acc[route.area] ?? [];
    acc[route.area].push(route);
    return acc;
  }, {});

  return (
    <section className="platform-map" id="platform-map">
      <div className="section-header">
        <h2>Platform map</h2>
        <p>Every route accessible from the homepage — preview pages work today, planned items show what&apos;s next in V0.</p>
      </div>
      <div className="platform-grid">
        {Object.entries(grouped).map(([area, areaRoutes]) => (
          <div key={area} className="platform-area">
            <h3>{area}</h3>
            <ul>
              {areaRoutes.map((route) => (
                <li key={route.path}>
                  <a href={resolveHref(route.path)} className="platform-route">
                    <span className="route-label">{route.label}</span>
                    <span className={`route-status status-${route.status}`}>{statusLabels[route.status]}</span>
                  </a>
                  <p className="route-desc">{route.description}</p>
                  <code className="route-path">{route.path}</code>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  );
}
