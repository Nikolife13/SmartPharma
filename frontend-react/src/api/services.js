import api from './axios';

// One thin wrapper object per backend controller - keeps every raw endpoint path
// in this single file instead of scattered across pages/components.
export const authService = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (username, password, role, email) =>
    api.post('/auth/register', { username, password, role, email }),
};

export const productService = {
  getAll: () => api.get('/products'),
  create: (product) => api.post('/products', product),
  updateStock: (id, quantityChange, reason) =>
    api.put(`/products/${id}/stock`, { quantityChange, reason }),
};

export const dashboardService = {
  getSummary: () => api.get('/dashboard'),
};

export const ordersService = {
  // ML-generated reorder suggestions (read-only) - separate from the real order
  // workflow below, mirroring the OrdersController/OrderController split on the backend.
  getSuggestions: () => api.get('/orders/suggestions'),
  create: (supplierId, items) => api.post('/orders', { supplierId, items }),
  getAll: () => api.get('/orders'),
  getById: (id) => api.get(`/orders/${id}`),
  respond: (id, items, expectedDeliveryDate, supplierNote) =>
    api.post(`/orders/${id}/respond`, { items, expectedDeliveryDate, supplierNote }),
};

export const supplierAdminService = {
  list: (status) => api.get('/admin/suppliers', { params: status ? { status } : {} }),
  updateStatus: (id, status) => api.patch(`/admin/suppliers/${id}/status`, { status }),
};

export const analyticsService = {
  getSummary: (period) => api.get('/analytics/summary', { params: { period } }),
};
