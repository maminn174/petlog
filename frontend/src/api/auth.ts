import {apiClient} from "@/api/client";

export type CsrfToken = {
  parameterName: string
  headerName: string
  token: string
}

type LoginRequest = {
  email: string
  password: string
}

export type User = {
  id: number
  name: string
  email: string
  createdAt: string
}

export const getCurrentUser = async (): Promise<User> => {
  const response = await apiClient.get<User>('/auth/me')

  return response.data
}

export const getCsrfToken = async () => {
  const response = await apiClient.get<CsrfToken>('/auth/csrf')

  return response.data
}

export const login = async (data: LoginRequest) => {
  const csrf = await getCsrfToken()
  const response = await apiClient.post(
    '/auth/login',
    data,
    {
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  )

  return response.data
}