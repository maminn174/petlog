import PetCard from '@/components/pets/PetCard'
import { useQuery } from '@tanstack/react-query'
import { getPets } from '@/api/pets'

function PetsPage() {

  const {
    data: pets,
    isPending,
    isError,
  } = useQuery({
    queryKey: ['pets'],
    queryFn: getPets,
  })

  if (isPending) {
    return <p>Загрузка питомцев...</p>
  }

  if (isError) {
    return <p>Не удалось загрузить питомцев</p>
  }

  return (
    <div>
      <h1>Мои питомцы</h1>

      {pets.length === 0 && <h2>У вас пока нет питомцев</h2>}

      {pets.map(({ id, name, photo }) => (
        <PetCard key={id} id={id} name={name} photo={photo} />
      ))}
    </div>
  )
}

export default PetsPage
