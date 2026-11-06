import { Navigate, Outlet } from 'react-router'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'

function ProtectedRoute() {
  const { isPending, isError, error } = useCurrentUser()

  if (isPending) {
    return <p>Проверка авторизации...</p>
  }

  if (isError) {
    const status = axios.isAxiosError(error) ? error.response?.status : undefined

    if (status === 401 || status === 403) {
      return <Navigate to="/login" replace />
    }

    return <p>Не удалось проверить авторизацию</p>
  }

  return <Outlet />
}

export default ProtectedRoute