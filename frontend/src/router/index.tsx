import { createBrowserRouter } from 'react-router'
import LoginPage from '@/pages/LoginPage'
import PetsPage from '@/pages/PetsPage'
import AppLayout from '@/layouts/AppLayout'
import SettingsPage from '@/pages/SettingsPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <AppLayout />,
    children: [
      {
        path: '/pets',
        element: <PetsPage />,
      },
      {
        path: '/settings',
        element: <SettingsPage />,
      },
    ],
  },
])
