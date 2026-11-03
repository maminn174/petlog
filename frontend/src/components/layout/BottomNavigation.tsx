import { NavLink } from 'react-router'
import { PawPrint, Settings } from 'lucide-react'

function BottomNavigation() {
  return (
    <nav className="flex items-center justify-around border-t px-4 py-3">
      <NavLink
        to="/pets"
        className={({ isActive }) =>
          `flex flex-col items-center gap-1 text-xs ${
            isActive ? 'text-primary' : 'text-muted-foreground'
          }`
        }
      >
        <PawPrint size={22} />
        <span>Питомцы</span>
      </NavLink>
      <NavLink
        to="/settings"
        className={({ isActive }) =>
          `flex flex-col items-center gap-1 text-xs ${
            isActive ? 'text-primary' : 'text-muted-foreground'
          }`
        }
      >
        <Settings size={22} />
        <span>Настройки</span>
      </NavLink>
    </nav>
  )
}

export default BottomNavigation
