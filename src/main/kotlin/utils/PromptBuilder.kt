package com.kevin.utils

import com.kevin.model.dto.ArtworkResponse
import com.kevin.model.dto.FewShotExample
import com.kevin.model.dto.UserContextResponse
import com.kevin.model.dto.UserPreferencesResponse

/**
 * PromptBuilder
 * Builds structured prompts for Gemini API using user context and artwork information
 */
object PromptBuilder {

    /**
     * Build a complete prompt for docent generation
     *
     * @param artwork Artwork information
     * @param userPreferences User preferences (narrative style, length)
     * @param userContexts User contexts (memories, emotions, goals)
     * @param companionContexts Companion contexts (optional)
     * @param fewShotExamples Few-shot examples (optional)
     * @param customPrompt Custom instructions (optional)
     * @return Complete prompt string
     */
    fun buildDocentPrompt(
        artwork: ArtworkResponse,
        userPreferences: UserPreferencesResponse,
        userContexts: List<UserContextResponse> = emptyList(),
        companionContexts: List<UserContextResponse> = emptyList(),
        fewShotExamples: List<FewShotExample> = emptyList(),
        customPrompt: String? = null
    ): PromptComponents {
        val persona = buildPersona(userPreferences)
        val task = buildTask(userPreferences)
        val context = buildContext(artwork, userContexts, companionContexts)
        val form = buildForm(userPreferences)
        val fewShotSection = buildFewShotSection(fewShotExamples)

        val fullPrompt = buildString {
            appendLine("# AI 도슨트 생성 프롬프트")
            appendLine()
            appendLine("## 페르소나 (Persona)")
            appendLine(persona)
            appendLine()
            appendLine("## 작업 (Task)")
            appendLine(task)
            appendLine()
            appendLine("## 맥락 (Context)")
            appendLine(context)
            appendLine()
            appendLine("## 형식 (Form)")
            appendLine(form)
            appendLine()

            if (fewShotSection.isNotBlank()) {
                appendLine("## 참고 예시 (Few-Shot Examples)")
                appendLine(fewShotSection)
                appendLine()
            }

            if (!customPrompt.isNullOrBlank()) {
                appendLine("## 추가 요청사항")
                appendLine(customPrompt)
                appendLine()
            }

            appendLine("## 출력")
            appendLine("위의 정보를 바탕으로 도슨트 해설을 작성해주세요.")
        }

        return PromptComponents(
            fullPrompt = fullPrompt,
            persona = persona,
            task = task,
            context = context,
            form = form
        )
    }

    /**
     * Build persona section based on user preferences
     */
    private fun buildPersona(userPreferences: UserPreferencesResponse): String {
        val style = when (userPreferences.narrativeStyle.uppercase()) {
            "LITERARY" -> "당신은 문학적이고 감성적인 표현을 사용하는 미술 도슨트입니다. 작품을 시적으로 해석하고 깊은 감동을 전달합니다."
            "CONVERSATIONAL" -> "당신은 친근하고 대화하듯 설명하는 미술 도슨트입니다. 일상적인 언어로 작품을 쉽게 설명합니다."
            "POETIC" -> "당신은 시적이고 은유적인 언어를 사용하는 미술 도슨트입니다. 작품을 아름다운 언어로 표현합니다."
            "ANALYTICAL" -> "당신은 분석적이고 학술적인 미술 도슨트입니다. 작품의 기법, 역사적 배경, 의미를 체계적으로 설명합니다."
            else -> "당신은 전문적이면서도 따뜻한 미술 도슨트입니다."
        }

        return buildString {
            appendLine(style)
            if (userPreferences.isVisuallyImpaired) {
                appendLine("사용자는 시각 장애가 있으므로, 작품의 시각적 요소를 상세하게 언어로 표현해주세요.")
                appendLine("색상, 형태, 구도, 질감 등을 생생하게 묘사하여 작품을 상상할 수 있도록 도와주세요.")
            }
        }
    }

    /**
     * Build task section based on user preferences
     */
    private fun buildTask(userPreferences: UserPreferencesResponse): String {
        val length = when (userPreferences.preferredLength.uppercase()) {
            "SHORT" -> "짧고 간결하게 (약 200-300자)"
            "MEDIUM" -> "적당한 길이로 (약 400-600자)"
            "LONG" -> "상세하고 깊이 있게 (약 800-1200자)"
            else -> "적당한 길이로 (약 400-600자)"
        }

        return buildString {
            appendLine("주어진 작품에 대해 개인화된 도슨트 해설을 생성해주세요.")
            appendLine("길이: $length")
            appendLine()
            appendLine("다음 요소를 포함해주세요:")
            appendLine("1. 작품의 기본 정보 (제목, 작가, 시대)")
            appendLine("2. 작품의 시각적 특징 (색상, 구도, 형태)")
            appendLine("3. 작품의 의미와 메시지")
            appendLine("4. 사용자의 개인적 맥락과 연결")
            appendLine("5. 감상 포인트 및 감정적 공감")
        }
    }

    /**
     * Build context section with artwork and user information
     */
    private fun buildContext(
        artwork: ArtworkResponse,
        userContexts: List<UserContextResponse>,
        companionContexts: List<UserContextResponse>
    ): String {
        return buildString {
            appendLine("### 작품 정보")
            appendLine("- 제목: ${artwork.title}")
            artwork.titleEn?.let { appendLine("- 영문 제목: $it") }
            appendLine("- 작가: ${artwork.artist}")
            artwork.artistEn?.let { appendLine("- Artist: $it") }
            artwork.creationYear?.let { appendLine("- 제작 연도: $it") }
            artwork.creationPeriod?.let { appendLine("- 시대: $it") }
            artwork.medium?.let { appendLine("- 재료/기법: $it") }
            artwork.dimensions?.let { appendLine("- 크기: $it") }
            artwork.genre?.let { appendLine("- 장르: $it") }
            artwork.artworkType?.let { appendLine("- 유형: $it") }
            artwork.museum?.let { appendLine("- 소장: $it") }
            artwork.description?.let {
                appendLine("- 작품 설명: $it")
            }
            artwork.historicalContext?.let {
                appendLine("- 역사적 맥락: $it")
            }

            if (userContexts.isNotEmpty()) {
                appendLine()
                appendLine("### 사용자 개인 맥락")
                userContexts.forEach { context ->
                    appendLine("- [${context.contextType}] ${context.title ?: ""}")
                    appendLine("  ${context.content}")
                    context.emotionalTone?.let { appendLine("  감정: $it") }
                }
            }

            if (companionContexts.isNotEmpty()) {
                appendLine()
                appendLine("### 동행자 관찰 및 의견")
                companionContexts.forEach { context ->
                    appendLine("- ${context.title ?: ""}")
                    appendLine("  ${context.content}")
                }
            }
        }
    }

    /**
     * Build form section
     */
    private fun buildForm(userPreferences: UserPreferencesResponse): String {
        return buildString {
            appendLine("출력 형식:")
            appendLine("- 한국어로 작성")
            appendLine("- 자연스러운 음성 낭독에 적합한 문장 구조")
            appendLine("- 특수 문자나 기호 최소화")
            appendLine("- 문단 구분을 명확히 (각 주제별로 줄바꿈)")

            when (userPreferences.narrativeStyle.uppercase()) {
                "LITERARY" -> appendLine("- 문학적이고 감성적인 표현 사용")
                "CONVERSATIONAL" -> appendLine("- 대화체로 친근하게 작성")
                "POETIC" -> appendLine("- 시적이고 은유적인 표현 사용")
                "ANALYTICAL" -> appendLine("- 학술적이고 체계적인 설명")
            }
        }
    }

    /**
     * Build few-shot examples section
     */
    private fun buildFewShotSection(examples: List<FewShotExample>): String {
        if (examples.isEmpty()) return ""

        return buildString {
            appendLine("다음은 높은 평가를 받은 도슨트 해설 예시입니다:")
            appendLine()
            examples.forEachIndexed { index, example ->
                appendLine("### 예시 ${index + 1}")
                appendLine("사용자 맥락: ${example.userContextSummary}")
                appendLine()
                appendLine("해설:")
                appendLine(example.exemplarText)
                appendLine()
                appendLine("---")
                appendLine()
            }
            appendLine("위 예시의 스타일과 깊이를 참고하되, 현재 작품과 사용자 맥락에 맞게 작성해주세요.")
        }
    }

    /**
     * Prompt components for storage
     */
    data class PromptComponents(
        val fullPrompt: String,
        val persona: String,
        val task: String,
        val context: String,
        val form: String
    )
}