import { PawPrint } from 'lucide-react'

type PetCardProps = {
  id: number
  name: string
  photo: string | null
}

function PetCard({ name, photo }: PetCardProps) {
  return(
    <div>
      <h2>{name}</h2>
      {photo ? (
        <img src={photo} alt={name} />
      ) : (
        <PawPrint size={32} />
      )}
    </div>
  )
}

export default PetCard