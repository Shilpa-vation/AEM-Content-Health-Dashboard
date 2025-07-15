import { useMemo } from 'react';

export default function useLinear(data = []) {
    console.log("useLinear", data);
  const linears = useMemo(() => {
    if (!Array.isArray(data) || data.length === 0) return [];

    const typeCounts = data.reduce((acc, item) => {
      acc[item.type] = (acc[item.type] || 0) + 1;
      return acc;
    }, {});

    return Object.entries(typeCounts).map(([key, value]) => ({
      name: key,
      count: value,
      width: (value / data.length) * 100,
      bg:
        key === 'metadata'
          ? '#FFF8F3'
          : key === 'seo'
          ? '#EEEDFF'
          : key === 'audit'
          ? '#F8E7FE'
          : key === 'workflow'
          ? '#FFF3F3'
          : key === 'brokenlinks'
          ? '#F1FFF9'
          : '#EEF7EE',
      color:
        key === 'metadata'
          ? '#FFB780'
          : key === 'seo'
          ? '#766FFF'
          : key === 'audit'
          ? '#B90EF2'
          : key === 'workflow'
          ? '#FF8082'
          : key === 'brokenlinks'
          ? '#6FFFC3'
          : '#4CAF50',
    }));
  }, [data]);

  console.log("linears", linears)

  return linears;
}
