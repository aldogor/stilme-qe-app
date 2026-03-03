package com.aldogor.stilme_qe_app.questionnaire

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.ItemQuestionBinding
import com.aldogor.stilme_qe_app.databinding.ItemScaleGroupBinding
import com.aldogor.stilme_qe_app.study.BranchingOperator
import com.aldogor.stilme_qe_app.study.QuestionDefinition
import com.aldogor.stilme_qe_app.study.QuestionType
import com.aldogor.stilme_qe_app.study.QuestionnaireResponses

/**
 * RecyclerView adapter for displaying questionnaire questions.
 *
 * Supports two display modes:
 * 1. Individual questions (RADIO, YESNO, TEXT) - shown as single cards
 * 2. Scale groups (SCALE type with scaleGroup) - shown as compact grid with icons
 *
 * Features:
 * - Handles branching logic (shows/hides questions based on previous responses)
 * - Tracks answered questions and calculates progress percentage
 * - Highlights answered questions with green border for visual feedback
 * - Scale groups use icons (empty to filled circles) with legend
 */
class QuestionAdapter(
    private val responses: QuestionnaireResponses,
    private val onResponseChanged: () -> Unit,
    private val onEligibilityCheck: (String, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_QUESTION = 0
        private const val VIEW_TYPE_SCALE_GROUP = 1
    }

    private var allQuestions: List<QuestionDefinition> = emptyList()
    private var displayItems: List<DisplayItem> = emptyList()

    /**
     * Represents an item to display in the RecyclerView.
     * Can be either a single question or a group of scale questions.
     */
    sealed class DisplayItem {
        data class SingleQuestion(
            val question: QuestionDefinition,
            val displayNumber: Int
        ) : DisplayItem()

        data class ScaleGroup(
            val scaleGroup: String,
            val title: String,
            val instructions: String,
            val questions: List<QuestionDefinition>,
            val options: List<com.aldogor.stilme_qe_app.study.QuestionOption>,
            val startNumber: Int
        ) : DisplayItem()
    }

    fun setQuestions(questions: List<QuestionDefinition>) {
        allQuestions = questions
        updateDisplayItems()
    }

    fun updateVisibleQuestions() {
        updateDisplayItems()
    }

    private fun updateDisplayItems() {
        val visibleQuestions = allQuestions.filter { isQuestionVisible(it) }
        val items = mutableListOf<DisplayItem>()
        var questionNumber = 1
        var i = 0

        while (i < visibleQuestions.size) {
            val question = visibleQuestions[i]

            // Check if this is a scale question that should be grouped
            if (question.type == QuestionType.SCALE && question.scaleGroup != null) {
                // Collect all questions in this scale group
                val groupQuestions = mutableListOf<QuestionDefinition>()
                val scaleGroup = question.scaleGroup
                var j = i

                while (j < visibleQuestions.size &&
                       visibleQuestions[j].scaleGroup == scaleGroup &&
                       visibleQuestions[j].type == QuestionType.SCALE) {
                    groupQuestions.add(visibleQuestions[j])
                    j++
                }

                // Create a scale group item
                items.add(DisplayItem.ScaleGroup(
                    scaleGroup = scaleGroup,
                    title = getScaleTitle(scaleGroup),
                    instructions = question.scaleInstructions ?: "",
                    questions = groupQuestions,
                    options = question.options,
                    startNumber = questionNumber
                ))

                questionNumber += groupQuestions.size
                i = j
            } else {
                // Single question
                items.add(DisplayItem.SingleQuestion(question, questionNumber))
                questionNumber++
                i++
            }
        }

        displayItems = items
        notifyDataSetChanged()
        onResponseChanged()
    }

    private fun getScaleTitle(scaleGroup: String): String {
        return when {
            scaleGroup.startsWith("bsmas") -> "BSMAS - Bergen Social Media Addiction Scale"
            scaleGroup.startsWith("phq9") -> "PHQ-9 - Patient Health Questionnaire"
            scaleGroup.startsWith("gad7") -> "GAD-7 - General Anxiety Disorder"
            scaleGroup.startsWith("fomo") -> "FoMO - Fear of Missing Out"
            scaleGroup.startsWith("pss10") -> "PSS-10 - Perceived Stress Scale"
            scaleGroup.startsWith("substance") -> "Uso di Sostanze"
            else -> scaleGroup.uppercase()
        }
    }

    private fun isQuestionVisible(question: QuestionDefinition): Boolean {
        val condition = question.showIf ?: return true
        val dependentValue = responses.getResponse(condition.dependsOn) ?: return false

        return when (condition.operator) {
            BranchingOperator.EQUALS -> dependentValue == condition.value
            BranchingOperator.NOT_EQUALS -> dependentValue != condition.value
            BranchingOperator.IN -> (condition.value as? Collection<*>)?.contains(dependentValue) == true
            BranchingOperator.NOT_IN -> (condition.value as? Collection<*>)?.contains(dependentValue) != true
            BranchingOperator.GREATER_THAN -> (dependentValue as? Int ?: 0) > (condition.value as? Int ?: 0)
            BranchingOperator.LESS_THAN -> (dependentValue as? Int ?: 0) < (condition.value as? Int ?: 0)
        }
    }

    fun getVisibleQuestionCount(): Int {
        return displayItems.sumOf { item ->
            when (item) {
                is DisplayItem.SingleQuestion -> 1
                is DisplayItem.ScaleGroup -> item.questions.size
            }
        }
    }

    fun getAnsweredCount(): Int {
        return displayItems.sumOf { item ->
            when (item) {
                is DisplayItem.SingleQuestion -> {
                    if (isQuestionAnswered(item.question)) 1 else 0
                }
                is DisplayItem.ScaleGroup -> {
                    item.questions.count { isQuestionAnswered(it) }
                }
            }
        }
    }

    private fun isQuestionAnswered(question: QuestionDefinition): Boolean {
        return when (question.type) {
            QuestionType.TEXT -> !responses.getStringResponse(question.variableName).isNullOrBlank()
            else -> responses.getIntResponse(question.variableName) != null
        }
    }

    fun areAllRequiredAnswered(): Boolean {
        return displayItems.all { item ->
            when (item) {
                is DisplayItem.SingleQuestion -> {
                    !item.question.required || isQuestionAnswered(item.question)
                }
                is DisplayItem.ScaleGroup -> {
                    item.questions.all { q -> !q.required || isQuestionAnswered(q) }
                }
            }
        }
    }

    fun getFirstUnansweredPosition(): Int {
        return displayItems.indexOfFirst { item ->
            when (item) {
                is DisplayItem.SingleQuestion -> {
                    item.question.required && !isQuestionAnswered(item.question)
                }
                is DisplayItem.ScaleGroup -> {
                    item.questions.any { q -> q.required && !isQuestionAnswered(q) }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.SingleQuestion -> VIEW_TYPE_QUESTION
            is DisplayItem.ScaleGroup -> VIEW_TYPE_SCALE_GROUP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SCALE_GROUP -> {
                val binding = ItemScaleGroupBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ScaleGroupViewHolder(binding)
            }
            else -> {
                val binding = ItemQuestionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                QuestionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.SingleQuestion -> {
                (holder as QuestionViewHolder).bind(item.question, item.displayNumber)
            }
            is DisplayItem.ScaleGroup -> {
                (holder as ScaleGroupViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    // ========================================================================
    // SINGLE QUESTION VIEW HOLDER
    // ========================================================================

    inner class QuestionViewHolder(
        private val binding: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentQuestion: QuestionDefinition? = null
        private var textWatcher: TextWatcher? = null

        fun bind(question: QuestionDefinition, questionNumber: Int) {
            currentQuestion = question

            binding.textQuestionNumber.text = questionNumber.toString()
            binding.textScaleInstructions.visibility = View.GONE
            binding.textQuestion.text = question.label

            if (question.helperText != null) {
                binding.textHelper.visibility = View.VISIBLE
                binding.textHelper.text = question.helperText
            } else {
                binding.textHelper.visibility = View.GONE
            }

            // All questions are mandatory - no need for asterisks
            binding.textRequired.visibility = View.GONE
            updateCardState(question)

            when (question.type) {
                QuestionType.RADIO, QuestionType.YESNO, QuestionType.SCALE -> {
                    setupRadioOptions(question)
                    binding.radioGroup.visibility = View.VISIBLE
                    binding.textInputLayout.visibility = View.GONE
                }
                QuestionType.TEXT -> {
                    setupTextInput(question)
                    binding.radioGroup.visibility = View.GONE
                    binding.textInputLayout.visibility = View.VISIBLE
                }
            }
        }

        private fun updateCardState(question: QuestionDefinition) {
            val isAnswered = isQuestionAnswered(question)
            if (isAnswered) {
                binding.cardQuestion.strokeWidth = 2
                binding.cardQuestion.strokeColor = ContextCompat.getColor(
                    binding.root.context, R.color.question_answered_stroke
                )
            } else {
                binding.cardQuestion.strokeWidth = 0
            }
        }

        private fun setupRadioOptions(question: QuestionDefinition) {
            binding.radioGroup.removeAllViews()
            binding.radioGroup.setOnCheckedChangeListener(null)

            val previousValue = responses.getIntResponse(question.variableName)

            question.options.forEach { option ->
                val radioButton = RadioButton(binding.root.context).apply {
                    text = option.label
                    id = View.generateViewId()
                    tag = option.value
                    textSize = 15f
                    setPadding(16, 20, 16, 20)
                    isChecked = previousValue == option.value
                }
                binding.radioGroup.addView(radioButton)
            }

            binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
                if (checkedId != -1) {
                    val selectedButton = group.findViewById<RadioButton>(checkedId)
                    val value = selectedButton.tag as Int
                    responses.setResponse(question.variableName, value)
                    updateCardState(question)
                    updateDisplayItems()
                    onEligibilityCheck(question.variableName, value)
                }
            }
        }

        private fun setupTextInput(question: QuestionDefinition) {
            textWatcher?.let { binding.editTextAnswer.removeTextChangedListener(it) }

            val previousValue = responses.getStringResponse(question.variableName)
            binding.editTextAnswer.setText(previousValue ?: "")

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        responses.setResponse(question.variableName, text)
                    }
                    updateCardState(question)
                    onResponseChanged()
                }
            }
            binding.editTextAnswer.addTextChangedListener(textWatcher)
        }
    }

    // ========================================================================
    // SCALE GROUP VIEW HOLDER
    // ========================================================================

    inner class ScaleGroupViewHolder(
        private val binding: ItemScaleGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem.ScaleGroup) {
            binding.textScaleTitle.text = item.title
            binding.textScaleInstructions.text = item.instructions

            // Setup legend (full width with numbers and text)
            setupLegend(item.options)

            // Setup question rows
            setupQuestionRows(item)

            // Update completion indicator
            updateCompletionIndicator(item)

            // Update card state
            updateCardState(item)
        }

        private fun setupLegend(options: List<com.aldogor.stilme_qe_app.study.QuestionOption>) {
            binding.legendItems.removeAllViews()
            val context = binding.root.context
            val density = context.resources.displayMetrics.density

            // Create legend items with number and text label (aligned to top)
            options.forEachIndexed { index, option ->
                val legendItem = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                }

                // Number in circle (same size as radio buttons - 32dp)
                val size = (32 * density).toInt()
                val numberView = TextView(context).apply {
                    text = (index + 1).toString()
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                    background = ContextCompat.getDrawable(context, R.drawable.bg_legend_number)
                    layoutParams = LinearLayout.LayoutParams(size, size)
                }
                legendItem.addView(numberView)

                // Text label below the number
                val labelView = TextView(context).apply {
                    text = option.label
                    textSize = 9f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 2
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (2 * density).toInt()
                    }
                }
                legendItem.addView(labelView)

                binding.legendItems.addView(legendItem)
            }
        }

        private fun setupQuestionRows(item: DisplayItem.ScaleGroup) {
            binding.questionsContainer.removeAllViews()
            val context = binding.root.context
            val inflater = LayoutInflater.from(context)

            item.questions.forEachIndexed { index, question ->
                val rowView = inflater.inflate(R.layout.item_scale_row, binding.questionsContainer, false)

                val numberView = rowView.findViewById<TextView>(R.id.text_row_number)
                val questionView = rowView.findViewById<TextView>(R.id.text_row_question)
                val radioContainer = rowView.findViewById<LinearLayout>(R.id.radio_container)

                numberView.text = (item.startNumber + index).toString()
                questionView.text = question.label

                // Setup number-based radio buttons
                setupRowRadioButtons(radioContainer, question, item.options)

                // Highlight row if answered
                val isAnswered = responses.getIntResponse(question.variableName) != null
                if (isAnswered) {
                    rowView.setBackgroundColor(ContextCompat.getColor(context, R.color.row_answered_bg))
                }

                binding.questionsContainer.addView(rowView)
            }
        }

        private fun setupRowRadioButtons(
            container: LinearLayout,
            question: QuestionDefinition,
            options: List<com.aldogor.stilme_qe_app.study.QuestionOption>
        ) {
            container.removeAllViews()
            val context = container.context
            val selectedValue = responses.getIntResponse(question.variableName)
            val density = context.resources.displayMetrics.density

            options.forEachIndexed { index, option ->
                val numberContainer = LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    gravity = android.view.Gravity.CENTER
                }

                val isSelected = selectedValue == option.value
                // Use consistent size of 32dp for all buttons
                val size = (32 * density).toInt()

                val numberView = TextView(context).apply {
                    text = (index + 1).toString()
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(size, size)

                    if (isSelected) {
                        setTextColor(ContextCompat.getColor(context, R.color.on_primary))
                        background = ContextCompat.getDrawable(context, R.drawable.bg_number_selected)
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        background = ContextCompat.getDrawable(context, R.drawable.bg_number_unselected)
                    }

                    // Click handler
                    setOnClickListener {
                        responses.setResponse(question.variableName, option.value)
                        // Refresh the entire group
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            notifyItemChanged(position)
                        }
                        onResponseChanged()
                    }
                }

                numberContainer.addView(numberView)
                container.addView(numberContainer)
            }
        }

        private fun updateCompletionIndicator(item: DisplayItem.ScaleGroup) {
            val answered = item.questions.count { isQuestionAnswered(it) }
            val total = item.questions.size
            binding.textCompletion.text = binding.root.context.getString(R.string.scale_responses_count, answered, total)

            // Change color based on completion
            val color = when {
                answered == total -> R.color.question_answered_stroke
                answered > 0 -> android.R.color.holo_orange_dark
                else -> android.R.color.darker_gray
            }
            binding.textCompletion.setTextColor(ContextCompat.getColor(binding.root.context, color))
        }

        private fun updateCardState(item: DisplayItem.ScaleGroup) {
            val allAnswered = item.questions.all { isQuestionAnswered(it) }
            if (allAnswered) {
                binding.cardScaleGroup.strokeWidth = 2
                binding.cardScaleGroup.strokeColor = ContextCompat.getColor(
                    binding.root.context, R.color.question_answered_stroke
                )
            } else {
                binding.cardScaleGroup.strokeWidth = 0
            }
        }
    }
}
