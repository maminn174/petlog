import { useMutation, useQueryClient } from '@tanstack/react-query'
import { login } from '@/api/auth'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router'

const loginSchema = z.object({
  email: z.email('Некорректный email'),
  password: z.string().min(1, 'Введите пароль'),
})

type LoginFormValues = z.infer<typeof loginSchema>

function LoginPage() {
  const navigate = useNavigate()

  const queryClient = useQueryClient()

  const loginMutation = useMutation({
    mutationFn: login,

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['currentUser'],
      })

      navigate('/pets', { replace: true })
    },
  })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = (data: LoginFormValues) => {
    loginMutation.mutate(data)
  }

  return (
    <>
      <h1>Login Page</h1>
      <form onSubmit={handleSubmit(onSubmit)}>
        <label htmlFor="email">Введите email</label>
        <input id="email" type="email" {...register('email')} />
        {errors.email && <p>{errors.email.message}</p>}

        <label htmlFor="password">Введите пароль</label>
        <input id="password" type="password" {...register('password')} />
        {errors.password && <p>{errors.password.message}</p>}

        <Button type="submit" disabled={loginMutation.isPending}>
          Войти
        </Button>
        {loginMutation.isError && <p>Не удалось войти. Проверьте email или пароль</p>}
      </form>
    </>
  )
}

export default LoginPage