package com.pampam.wakemeup.ui.search

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.pampam.wakemeup.R
import com.pampam.wakemeup.databinding.FragmentSearchBinding
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment() {
    private val viewModel by viewModel<SearchViewModel>()
    private lateinit var binding: FragmentSearchBinding

    private lateinit var navController: NavController

    private lateinit var recentPredictionsAdapter: RecentPredictionsAdapter
    private lateinit var remotePredictionsAdapter: RemotePredictionsAdapter

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

        navController = findNavController()

        searchTextInputLayout.setStartIconOnClickListener {
            navController.popBackStack()
        }

        recentPredictionsAdapter = RecentPredictionsAdapter().apply {
            onRecentPredictionSelect = { prediction ->
                viewModel.onPredictionSelect(prediction)
                navController.navigate(R.id.action_searching_to_session)
            }
            onRecentPredictionDelete = { prediction ->
                viewModel.onRecentPredictionDelete(prediction)
            }
        }
        remotePredictionsAdapter = RemotePredictionsAdapter().apply {
            onRemotePredictionSelect = { prediction ->
                viewModel.onPredictionSelect(prediction)
                navController.navigate(R.id.action_searching_to_session)
            }
        }
        val concatAdapter = ConcatAdapter().apply {
            addAdapter(recentPredictionsAdapter)
            addAdapter(remotePredictionsAdapter)
        }
        predictionsRecyclerView.apply {
            adapter = concatAdapter
            layoutManager = LinearLayoutManager(context)
            setOnScrollChangeListener { _, _, _, _, _ ->
                searchTextInputLayout.elevation =
                    if (predictionsRecyclerView.canScrollVertically(-1)) {
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            4.0f, resources.displayMetrics
                        )
                    } else {
                        0.0f
                    }
            }
        }

        viewModel.recentPredictions.observe(viewLifecycleOwner, Observer { recentPredictions ->
            recentPredictionsAdapter.submitList(recentPredictions)
        })

        viewModel.remotePredictions.observe(viewLifecycleOwner, Observer { remotePredictions ->
            remotePredictionsAdapter.updateRemotePredictions(remotePredictions)
        })
    }

    override fun onStart() {
        super.onStart()

        val inputMethodManager: InputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInputFromWindow(
            searchTextInputEditText.applicationWindowToken,
            InputMethodManager.SHOW_IMPLICIT, 0
        )
        searchTextInputEditText.requestFocus()
    }


}