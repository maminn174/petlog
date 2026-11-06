import { useForm } from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createPet } from '@/api/pets'

const petSchema = z.object({
  name: z.string().trim()
    .min(2, 'Имя должно содержать минимум 2 символа')
    .max(100, 'Максимум 100 символов'),
  species: z.string().trim()
    .min(1, 'Укажите вид питомца')
    .max(50, 'Максимум 50 символов'),
  breed: z.string().trim().max(100).optional(),
  gender: z.enum(['', 'MALE', 'FEMALE', 'UNKNOWN']),
  birthDate: z.string(),
})

type PetFormValues = z.infer<typeof petSchema>

function CreatePetPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const createPetMutation = useMutation({
    mutationFn: createPet,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['pets']
      })
      navigate('/pets')
    },
  })

  const {
    register,
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

  const onSubmit = (data: PetFormValues) => {
    const payload = {
      ...data,
      gender: data.gender || null,
      breed: data.breed || null,
      birthDate: data.birthDate || null,
    }

    createPetMutation.mutate(payload)
  }


  return (
    <div>
      <h1>Добавление питомца</h1>
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

        <Button
          type="submit"
          disabled={createPetMutation.isPending}
        >
          {createPetMutation.isPending
            ? 'Сохранение...'
            : 'Создать питомца'
          }
        </Button>
        {createPetMutation.isError && (
          <p role="alert">Не удалось создать питомца. Попробуйте еще раз.</p>
        )}
      </form>
    </div>
  )
}

export default CreatePetPage
