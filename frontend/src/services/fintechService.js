import api from './api.js'

export const listarUsuarios = () => api.get('/usuarios').then(({ data }) => data)
export const crearUsuario = (usuario) => api.post('/usuarios', usuario).then(({ data }) => data)
export const actualizarUsuario = (id, usuario) => api.put(`/usuarios/${id}`, usuario).then(({ data }) => data)
export const eliminarUsuario = (id) => api.delete(`/usuarios/${id}`).then(({ data }) => data)

export const listarBilleteras = (idUsuario) =>
  api.get('/billeteras', { params: { idUsuario } }).then(({ data }) => data)
export const crearBilletera = (billetera) => api.post('/billeteras', billetera).then(({ data }) => data)

export const ejecutarOperacion = (tipo, operacion) =>
  api.post(`/operaciones/${tipo}`, operacion).then(({ data }) => data)
export const consultarHistorial = (filtros) =>
  api.get('/historial', { params: filtros }).then(({ data }) => data)
export const listarNotificaciones = (idUsuario) =>
  api.get(`/notificaciones/usuarios/${idUsuario}`).then(({ data }) => data)

export function mensajeError(error) {
  const data = error?.response?.data
  if (data?.fieldErrors) {
    const campos = Object.values(data.fieldErrors).filter(Boolean).join(' ')
    if (campos) return campos
  }
  return data?.message || error?.message || 'Ocurrió un error inesperado.'
}
