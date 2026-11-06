import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { type Gender, getPetById } from '@/api/pets'
import { format } from 'date-fns'
import { Button } from '@/components/ui/button'

const genderLabels: Record<Gender, string> = {
  MALE: 'Мальчик',
  FEMALE: 'Девочка',
  UNKNOWN: 'Неизвестно',
}

function PetDetailsPage() {
  const { id } = useParams()
  const petId = Number(id)

  const isValidId =
    id !== undefined
    && Number.isSafeInteger(petId)
    && petId > 0

  const {
    data: pet,
    isPending,
    isError,
  } = useQuery({
    queryKey: ['pet', petId],
    queryFn: () => getPetById(petId),
    enabled: isValidId,
  })

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
      <h1>Имя: {pet.name}</h1>
      <p>Вид: {pet.species}</p>
      <p>Порода: {pet.breed ?? 'Не указано'}</p>
      <p>Пол: {pet.gender !== null ? genderLabels[pet.gender] : 'Не указано'}</p>
      <p>
        Дата рождения:{' '}
        {pet.birthDate ? format(new Date(pet.birthDate), 'dd.MM.yyyy') : 'Не указано'}
      </p>

      <Button
        render={<Link to={`/pets/${petId}/edit`} />}
      >
        Редактировать
      </Button>
    </>
  )
}

export default PetDetailsPage