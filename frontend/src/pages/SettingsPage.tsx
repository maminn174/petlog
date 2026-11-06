import { useMutation, useQueryClient } from '@tanstack/react-query'
import { logout } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router'

function SettingsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })


  return (
    <>
      <h1>Настройки</h1>
      <Button
        type="button"
        disabled={logoutMutation.isPending}
        onClick={() => logoutMutation.mutate()}
      >
        Выйти
      </Button>
      {logoutMutation.isError && (
        <p>Не удалось выйти из аккаунта</p>
      )}
    </>
  )
}

export default SettingsPage
