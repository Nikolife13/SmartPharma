import api from './axios';

export const authService = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (username, password) => api.post('/auth/register', { username, password }),
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
