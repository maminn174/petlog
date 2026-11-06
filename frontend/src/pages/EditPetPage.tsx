import { useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getPetById, updatePet, type UpdatePetRequest } from '@/api/pets'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useEffect } from 'react'
import { Button } from '@/components/ui/button'

const petSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Имя должно содержать минимум 2 символа')
    .max(100, 'Максимум 100 символов'),
  species: z.string().trim().min(1, 'Укажите вид питомца').max(50, 'Максимум 50 символов'),
  breed: z.string().trim().max(100).optional(),
  gender: z.enum(['', 'MALE', 'FEMALE', 'UNKNOWN']),
  birthDate: z.string(),
})

type PetFormValues = z.infer<typeof petSchema>

function EditPetPage() {
  const { id } = useParams()
  const petId = Number(id)

  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const isValidId = id !== undefined && Number.isSafeInteger(petId) && petId > 0

  const {
    data: pet,
    isPending,
    isError,
  } = useQuery({
    queryKey: ['pet', petId],
    queryFn: () => getPetById(petId),
    enabled: isValidId,
  })

  const updatePetMutation = useMutation({
    mutationFn: (data: UpdatePetRequest) => updatePet(petId, data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['pet', petId],
      })
      
      await queryClient.invalidateQueries({
        queryKey: ['pets']
      })

      navigate(`/pets/${petId}`, { replace: true })
    }
  })

  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm<PetFormValues>({
    resolver: zodResolver(petSchema),
    defaultValues: {
      name: '',
      species: '',
      breed: '',
      gender: '',
      birthDate: '',
    },
  })

  useEffect(() => {
    if (pet) {
      reset({
        name: pet.name,
        species: pet.species,
        breed: pet.breed ?? '',
        gender: pet.gender ?? '',
        birthDate: pet.birthDate ?? '',
      })
    }
  }, [pet, reset])

  const onSubmit = (data: PetFormValues) => {
    const payload = {
      ...data,
      gender: data.gender || null,
      breed: data.breed || null,
      birthDate: data.birthDate || null,
    }

    updatePetMutation.mutate(payload)
  }

  if (!isValidId) {
    return <p>Некорректный ID питомца</p>
  }

  if (isPending) {
    return <p>Загрузка питомца...</p>
  }

  if (isError) {
    return <p>Не удалось загрузить питомца</p>
  }

  return (
    <>
      <h1>Редактирование: {pet.name}</h1>
      <form onSubmit={handleSubmit(onSubmit)}>
        <label htmlFor="pet-name">Имя питомца</label>
        <input id="pet-name" {...register('name')} />
        {errors.name && <p>{errors.name.message}</p>}

        <label htmlFor="species">Вид питомца</label>
        <input id="species" {...register('species')} />
        {errors.species && <p>{errors.species.message}</p>}

        <label htmlFor="breed">Порода питомца</label>
        <input id="breed" {...register('breed')} />
        {errors.breed && <p>{errors.breed.message}</p>}

        <label htmlFor="gender">Пол питомца</label>
        <select id="gender" {...register('gender')}>
          <option value="">Выберите пол</option>
          <option value="MALE">Мальчик</option>
          <option value="FEMALE">Девочка</option>
          <option value="UNKNOWN">Неизвестно</option>
        </select>
        {errors.gender && <p>{errors.gender.message}</p>}

        <label htmlFor="birthDate">Дата рождения</label>
        <input
          type="date"
          id="birthDate"
          max={new Date().toLocaleDateString('en-CA')}
          {...register('birthDate')}
        />
        {errors.birthDate && <p>{errors.birthDate.message}</p>}

        <Button type="submit" disabled={updatePetMutation.isPending}>
          {updatePetMutation.isPending ? 'Сохранение...' : 'Сохранить изменения'}
        </Button>
        {updatePetMutation.isError && (
          <p role="alert">Не удалось сохранить изменения.</p>
        )}
      </form>
    </>
  )
}

export default EditPetPage
