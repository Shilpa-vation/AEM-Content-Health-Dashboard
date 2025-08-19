import React from "react"
export const Chart = ({ used, total }) => {
  const percentageUsed = ((used / total) * 100).toFixed(2);
  return (
    <div className="circular-progress" style={{ "--value": percentageUsed }}>
      <div className="label">{total} MB</div>
    </div>
  );
};
