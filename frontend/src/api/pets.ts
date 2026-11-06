import { apiClient } from '@/api/client'
import { getCsrfToken } from '@/api/auth'

type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN'

export type Pet = {
  id: number
  name: string
  species: string
  breed: string | null
  gender: Gender | null
  birthDate: string | null
  photo: string | null
  createdAt: string
  updatedAt: string
}

export type CreatePetRequest = {
  name: string
  species: string
  breed: string | null
  gender: Gender | null
  birthDate: string | null
}

export const getPets = async () => {
  const response = await apiClient.get<Pet[]>('/pets')

  return response.data
}

export const createPet = async (data: CreatePetRequest): Promise<Pet> => {
  const csrf = await getCsrfToken()
  const response = await apiClient.post<Pet>(
    '/pets',
    data,
    {
      headers: {
        [csrf.headerName]: csrf.token,
      },
    })

  return response.data
}