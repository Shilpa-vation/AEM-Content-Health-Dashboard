export const formatDateTime = (isoString) => {
    const date = new Date(isoString);
    const formatted = date.toLocaleString("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
    return formatted
}

export const formatDate = (isoString) => {
    const dateObj = new Date(isoString);
    return dateObj.toLocaleDateString("en-GB");
} 