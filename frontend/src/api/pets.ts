import { apiClient } from '@/api/client'

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

export const getPets = async () => {
  const response = await apiClient.get<Pet[]>('/pets')

  return response.data
}