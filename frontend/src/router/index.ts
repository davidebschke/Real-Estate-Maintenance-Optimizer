import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import OverviewView from '@/views/OverviewView.vue'
import CalendarView from '@/views/CalendarView.vue'
import StatisticsView from '@/views/StatisticsView.vue'
import PropertiesView from '@/views/PropertiesView.vue'

/** Identifier of a top-level page reachable from the main navigation. */
export type NavigationKey = 'overview' | 'calendar' | 'statistics' | 'properties'

/** Route definitions for the main application pages, shared between the app router and router-aware tests. */
export const routes: RouteRecordRaw[] = [
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
    path: '/statistics',
    name: 'statistics' satisfies NavigationKey,
    component: StatisticsView,
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

export default router
