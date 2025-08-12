import { useMemo } from 'react';

export default function useLinear(data = []) {
  const linears = useMemo(() => {
    const allTypes = ['metadata', 'seo', 'audit', 'workflow', 'brokenlinks'];

    const colors = {
      metadata: { bg: '#FFF8F3', color: '#FFB780' },
      seo: { bg: '#EEEDFF', color: '#766FFF' },
      audit: { bg: '#F8E7FE', color: '#B90EF2' },
      workflow: { bg: '#FFF3F3', color: '#FF8082' },
      brokenlinks: { bg: '#F1FFF9', color: '#6FFFC3' },
    };

    if (!Array.isArray(data) || data.length === 0) {
      return allTypes.map(name => ({
        name,
        count: 0,
        width: 0,
        bg: colors[name].bg,
        color: colors[name].color,
      }));
    }

    const typeCounts = data.reduce((acc, item) => {
      acc[item.type] = (acc[item.type] || 0) + 1;
      return acc;
    }, {});

    return allTypes.map(name => {
      const count = typeCounts[name] || 0;
      return {
        name,
        count,
        width: (count / data.length) * 100,
        bg: colors[name].bg,
        color: colors[name].color,
      };
    });
  }, [data]);

  return linears;
}
