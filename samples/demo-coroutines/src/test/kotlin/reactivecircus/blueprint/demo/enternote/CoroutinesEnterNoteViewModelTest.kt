package reactivecircus.blueprint.demo.enternote

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import reactivecircus.blueprint.demo.domain.interactor.CoroutinesCreateNote
import reactivecircus.blueprint.demo.domain.interactor.CoroutinesGetNoteByUuid
import reactivecircus.blueprint.demo.domain.interactor.CoroutinesUpdateNote
import reactivecircus.blueprint.demo.domain.model.Note
import reactivecircus.blueprint.demo.testutil.CoroutinesTestRule

@ExperimentalCoroutinesApi
class CoroutinesEnterNoteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private val noteUuid = "uuid"

    private val dummyNote = Note(
        uuid = noteUuid,
        content = "Note 1",
        timeCreated = System.currentTimeMillis(),
        timeLastUpdated = System.currentTimeMillis()
    )

    private val getNoteByUuid = mockk<CoroutinesGetNoteByUuid> {
        coEvery { execute(any()) } returns dummyNote
    }

    private val createNote = mockk<CoroutinesCreateNote> {
        coEvery { execute(any()) } returns Unit
    }

    private val updateNote = mockk<CoroutinesUpdateNote> {
        coEvery { execute(any()) } returns Unit
    }

    private val stateObserver = mockk<Observer<State>>(relaxed = true)

    private val viewModelCreateMode: CoroutinesEnterNoteViewModel by lazy {
        CoroutinesEnterNoteViewModel(
            noteUuid = null,
            getNoteByUuid = getNoteByUuid,
            createNote = createNote,
            updateNote = updateNote
        )
    }

    private val viewModelUpdateMode: CoroutinesEnterNoteViewModel by lazy {
        CoroutinesEnterNoteViewModel(
            noteUuid = noteUuid,
            getNoteByUuid = getNoteByUuid,
            createNote = createNote,
            updateNote = updateNote
        )
    }

    @Test
    fun `emit State with null value when initialized in create mode`() = runBlockingTest {
        viewModelCreateMode.noteLiveData.observeForever(stateObserver)

        coVerify(exactly = 0) {
            getNoteByUuid.execute(any())
        }

        verify(exactly = 1) {
            stateObserver.onChanged(
                State(null)
            )
        }
    }

    @Test
    fun `emit State with loaded Note when initialized in update mode`() = runBlockingTest {
        viewModelUpdateMode.noteLiveData.observeForever(stateObserver)

        coVerify(exactly = 1) {
            getNoteByUuid.execute(any())
        }

        verify(exactly = 1) {
            stateObserver.onChanged(
                State(dummyNote)
            )
        }
    }

    @Test
    fun `execute CreateNote with new note content`() = runBlockingTest {
        viewModelCreateMode.noteLiveData.observeForever(stateObserver)

        viewModelCreateMode.createNote(dummyNote.content)

        val slot = slot<CoroutinesCreateNote.Params>()

        coVerify(exactly = 1) {
            createNote.execute(params = capture(slot))
        }

        slot.captured.note.content shouldEqual dummyNote.content
    }

    @Test
    fun `execute UpdateNote with updated note`() = runBlockingTest {
        viewModelUpdateMode.noteLiveData.observeForever(stateObserver)

        val updatedNote = dummyNote.copy(content = "updated note")
        viewModelUpdateMode.updateNote(updatedNote)

        val slot = slot<CoroutinesUpdateNote.Params>()

        coVerify(exactly = 1) {
            updateNote.execute(params = capture(slot))
        }

        slot.captured.note shouldEqual updatedNote
    }
}
