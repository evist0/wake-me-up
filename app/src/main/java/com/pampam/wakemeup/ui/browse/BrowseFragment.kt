package com.pampam.wakemeup.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.pampam.wakemeup.R
import com.pampam.wakemeup.ui.MainViewModel
import com.pampam.wakemeup.ui.Padding
import kotlinx.android.synthetic.main.fragment_browse.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class BrowseFragment : Fragment() {
    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = Navigation.findNavController(view)

        searchCardView.apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                mainViewModel.setMapPadding(
                    Padding(
                        left = browseRootLayout.paddingLeft + searchCardView.marginLeft,
                        top = browseRootLayout.paddingTop,
                        right = browseRootLayout.paddingRight + searchCardView.marginRight,
                        bottom = browseRootLayout.paddingBottom + searchCardView.marginBottom + searchCardView.height
                    )
                )
            }

            setOnClickListener {
                navController.navigate(R.id.action_browse_to_searching)
            }
        }
    }
}