import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login.vue'
import Layout from '../components/Layout.vue'
import { clearSession, getToken, isTokenExpired } from '../utils/auth'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: Layout,
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '主控面板' }, component: () => import('../views/Dashboard.vue') },
      { path: 'accounts/:accountNumber', name: 'AccountDetail', meta: { title: '账户详情' }, component: () => import('../views/AccountDetail.vue') },
      { path: 'transfer', name: 'Transfer', meta: { title: '转账汇款' }, component: () => import('../views/Transfer.vue') },
      { path: 'records', name: 'Records', meta: { title: '交易流水' }, component: () => import('../views/Records.vue') },
      { path: 'profile', name: 'Profile', meta: { title: '个人中心' }, component: () => import('../views/Profile.vue') },
      { path: 'contacts', name: 'Contacts', meta: { title: '常用收款人' }, component: () => import('../views/Contacts.vue') },
      { path: 'products', name: 'Products', meta: { title: '存款理财' }, component: () => import('../views/Products.vue') },
      { path: 'loans', name: 'Loans', meta: { title: '贷款中心' }, component: () => import('../views/Loans.vue') },
      { path: 'credit-cards', name: 'CreditCards', meta: { title: '信用卡中心' }, component: () => import('../views/CreditCards.vue') },
      { path: 'notifications', name: 'Notifications', meta: { title: '通知中心' }, component: () => import('../views/Notifications.vue') },
      { path: 'agent', name: 'Agent', meta: { title: '智能助手' }, component: () => import('../views/Agent.vue') },
      { path: 'admin', name: 'Admin', meta: { title: '后台管理' }, component: () => import('../views/Admin.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const publicPages = ['/login', '/register']
  if (publicPages.includes(to.path)) {
    next()
    return
  }
  const token = getToken()
  if (!token || isTokenExpired(token)) {
    clearSession()
    next('/login')
    return
  }
  next()
})

export default router
