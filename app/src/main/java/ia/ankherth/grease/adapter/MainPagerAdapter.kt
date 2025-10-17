package ia.ankherth.grease.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ia.ankherth.grease.fragment.NewHomeFragment
import ia.ankherth.grease.fragment.NewHistoryFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NewHomeFragment() // Nueva pantalla Home con tarjeta destacada
            1 -> NewHistoryFragment() // Nueva pantalla Historial con tarjetas
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
