import React, { useEffect, useState } from 'react';
import useLinear from './hooks/useLinear';

export default function Select({ selectType, data, handleFilter, selectedValue }) {
    console.log("selectedValue", selectedValue)
    const lineData = useLinear(data);
    const [selectData, setSelectData] = useState([]);

    useEffect(() => {
        if (selectType === "type") {
            setSelectData(lineData)
        }
        else if (selectType === 'days') {
            setSelectData(data)
        }
        else {
            const uniqueStatuses = [...new Set(data.map(item => item.status))];
            setSelectData(uniqueStatuses?.map(status => ({ name: status })));
        }
    }, [data])

    return (
        <select value={selectedValue.toLowerCase()} style={{ textTransform: "capitalize" }} className="form-control select" onChange={(e) => handleFilter(selectType, e.target.value)}>
            <option value="all">All {selectType}</option>
            {selectData?.map((item, index) => (
                <option key={index} value={item.name.toLowerCase()}>{item.name}</option>
            ))}
        </select>
    )
}