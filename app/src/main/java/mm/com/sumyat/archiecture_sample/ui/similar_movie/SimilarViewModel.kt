package mm.com.sumyat.archiecture_sample.ui.similar_movie

import androidx.lifecycle.*
import androidx.lifecycle.Observer
import mm.com.sumyat.archiecture_sample.repository.DetailRepository
import mm.com.sumyat.archiecture_sample.testing.OpenForTesting
import mm.com.sumyat.archiecture_sample.util.AbsentLiveData
import mm.com.sumyat.archiecture_sample.vo.MDetail
import mm.com.sumyat.archiecture_sample.vo.Resource
import mm.com.sumyat.archiecture_sample.vo.Status
import javax.inject.Inject

@OpenForTesting
class SimilarViewModel @Inject constructor(private val repository: DetailRepository) :
    ViewModel() {

    private val _query = MutableLiveData<String>()

    var results: LiveData<Resource<List<MDetail>>> = Transformations
        .switchMap(_query) { search ->
            if (search.isNullOrBlank()) {
                AbsentLiveData.create()
            } else {
                repository.search(search.toInt())
            }
        }

    private val nextPageHandler =
        NextPageHandler(repository)

    var loadMoreStatus: LiveData<LoadMoreState> = nextPageHandler.loadMoreState

    fun setQuery(movie_id: String) {
        if (movie_id == _query.value) {
            return
        }
        nextPageHandler.reset()
        _query.value = movie_id
    }

    fun loadNextPage() {
        _query.value?.let {
            if (it.isNotBlank()) {
                nextPageHandler.queryNextPage(it)
            }
        }
    }

    fun refresh() {
        _query.value?.let {
            _query.value = it
        }
    }

    class NextPageHandler(private val repository: DetailRepository) : Observer<Resource<Boolean>> {
        private var nextPageLiveData: LiveData<Resource<Boolean>>? = null
        var loadMoreState = MutableLiveData<LoadMoreState>()
        private var query: String? = null
        private var _hasMore: Boolean = false
        val hasMore
            get() = _hasMore

        init {
            reset()
        }

        fun queryNextPage(query: String) {
            if (this.query == query) {
                return
            }
            unregister()
            this.query = query
            nextPageLiveData = repository.searchNextPage(query.toInt())
            loadMoreState.value =
                LoadMoreState(
                    isRunning = true,
                    errorMessage = null
                )
            nextPageLiveData?.observeForever(this)
        }

        override fun onChanged(result: Resource<Boolean>?) {
            if (result == null) {
                reset()
            } else {
                when (result.status) {
                    Status.SUCCESS -> {
                        _hasMore = result.data == true
                        unregister()
                        loadMoreState.setValue(
                            LoadMoreState(
                                isRunning = false,
                                errorMessage = null
                            )
                        )
                    }
                    Status.ERROR -> {
                        _hasMore = true
                        unregister()
                        loadMoreState.setValue(
                            LoadMoreState(
                                isRunning = false,
                                errorMessage = result.message
                            )
                        )
                    }
                    Status.LOADING -> {
                        // ignore
                    }
                }
            }
        }

        private fun unregister() {
            nextPageLiveData?.removeObserver(this)
            nextPageLiveData = null
            if (_hasMore) {
                query = null
            }
        }

        fun reset() {
            unregister()
            _hasMore = true
            loadMoreState.value =
                LoadMoreState(
                    isRunning = false,
                    errorMessage = null
                )
        }
    }

    class LoadMoreState(val isRunning: Boolean, val errorMessage: String?) {
        private var handledError = false

        val errorMessageIfNotHandled: String?
            get() {
                if (handledError) {
                    return null
                }
                handledError = true
                return errorMessage
            }
    }
}