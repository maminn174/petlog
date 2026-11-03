import { Outlet } from 'react-router'
import BottomNavigation from '@/components/layout/BottomNavigation'

function AppLayout() {
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="border-b px-4 py-3">PetLog</header>
      <main className="flex-1 px-4 py-6">
        <Outlet />
      </main>
      <BottomNavigation />
    </div>
  )
}

export default AppLayout
