package com.pampam.wakemeup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.mancj.materialsearchbar.MaterialSearchBar.OnSearchActionListener
import com.pampam.wakemeup.R
import com.pampam.wakemeup.databinding.FragmentSearchBinding
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment(), OnSearchActionListener {
    private lateinit var navController: NavController

    private val viewModel by viewModel<SearchViewModel>()
    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_search, container, false
        )

        binding.viewModel = this.viewModel
        binding.lifecycleOwner = this.viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        initSearchBar()
    }

    private fun initSearchBar() {
        val predictionsAdapter = DestinationPredictionsAdapter(layoutInflater).apply {
            onPredictionSelect = { prediction ->
                viewModel.endSearch(prediction)
                navController.navigate(R.id.action_searching_to_sessionDetails)
            }
            onPredictionDelete = { prediction ->
                viewModel.deleteRecentPrediction(prediction)
            }
        }
        searchBar.setCustomSuggestionAdapter(predictionsAdapter)
        searchBar.searchEditText.addTextChangedListener { editable ->
            viewModel.destinationSearchQuery.value = editable.toString()
        }
        searchBar.setOnSearchActionListener(this)
        searchBar.openSearch()

        observeIsSearching()
        observeSuggestedDestinations()
    }

    private fun observeIsSearching() {
        viewModel.isSearching.observe(viewLifecycleOwner, Observer { isSearching ->
            if (isSearching) {
                searchBar.openSearch()
            } else {
                searchBar.closeSearch()
            }
        })
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        if (enabled && viewModel.isSearching.value != enabled) {
            viewModel.beginSearch()
        } else if (!enabled && viewModel.isSearching.value != enabled) {
            viewModel.closeSearch()
        }
    }

    private fun observeSuggestedDestinations() {
        viewModel.suggestedDestinations.observe(
            viewLifecycleOwner,
            Observer { suggestedDestinations ->
                searchBar.updateLastSuggestions(suggestedDestinations)
            })
    }

    override fun onSearchConfirmed(query: CharSequence?) {}
    override fun onButtonClicked(buttonCode: Int) {}
}