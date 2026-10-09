import { createRouter, createWebHistory, RouterView, type RouteRecordRaw } from 'vue-router'
import OverviewView from '@/views/OverviewView.vue'
import CalendarView from '@/views/CalendarView.vue'
import StatisticsView from '@/views/StatisticsView.vue'
import PropertyStatisticsView from '@/views/PropertyStatisticsView.vue'
import SavingsStatisticsView from '@/views/SavingsStatisticsView.vue'
import OptimizationView from '@/views/OptimizationView.vue'
import PropertiesView from '@/views/PropertiesView.vue'
import LoginView from '@/views/LoginView.vue'
import { installAuthGuard } from '@/router/authGuard'

declare module 'vue-router' {
  interface RouteMeta {
    /** Reachable without a session, e.g. the login screen. */
    public?: boolean
    /** "auth" renders the page on its own, without the app header and footer. */
    layout?: 'auth'
  }
}

/** Identifier of a top-level page reachable from the main navigation. */
export type NavigationKey = 'overview' | 'calendar' | 'optimization' | 'statistics' | 'properties'

/** Route definitions for the main application pages, shared between the app router and router-aware tests. */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, layout: 'auth' },
  },
  {
    path: '/',
    name: 'overview' satisfies NavigationKey,
    component: OverviewView,
  },
  {
    path: '/calendar',
    name: 'calendar' satisfies NavigationKey,
    component: CalendarView,
  },
  {
    path: '/optimization',
    name: 'optimization' satisfies NavigationKey,
    component: OptimizationView,
  },
  {
    path: '/statistics',
    component: RouterView,
    children: [
      {
        path: '',
        name: 'statistics' satisfies NavigationKey,
        component: StatisticsView,
      },
      {
        path: 'properties',
        name: 'statistics-properties',
        component: PropertyStatisticsView,
      },
      {
        path: 'savings',
        name: 'statistics-savings',
        component: SavingsStatisticsView,
      },
    ],
  },
  {
    path: '/properties',
    name: 'properties' satisfies NavigationKey,
    component: PropertiesView,
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

installAuthGuard(router)

export default router
