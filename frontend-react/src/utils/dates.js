export function isNearExpiry(expiryDate, withinDays = 30) {
  const diffMs = new Date(expiryDate) - new Date();
  const diffDays = diffMs / (1000 * 60 * 60 * 24);
  return diffDays <= withinDays;
}

export function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString('en-IE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}
