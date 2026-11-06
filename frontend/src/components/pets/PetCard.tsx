import { PawPrint } from 'lucide-react'
import { Link } from 'react-router'

type PetCardProps = {
  id: number
  name: string
  photo: string | null
}

function PetCard({ id, name, photo }: PetCardProps) {
  return (
    <Link to={`/pets/${id}`}>
        <h2>{name}</h2>
        {photo ? <img src={photo} alt={name} /> : <PawPrint size={32} />}
    </Link>
  )
}

export default PetCard