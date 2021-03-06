package com.roihunter.facehunter.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.roihunter.facehunter.dto.slack.interactive.PayloadDto
import com.roihunter.facehunter.flow.EvaluateGuessFlow
import com.roihunter.facehunter.flow.NewGuessFlow
import com.roihunter.facehunter.flow.SendHelpInfoFlow
import com.roihunter.facehunter.manager.SlackManager
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class InteractiveController(
        private val mapper: ObjectMapper,
        private val evaluateGuessFlow: EvaluateGuessFlow,
        private val newGuessFlow: NewGuessFlow,
        private val sendHelpInfoFlow: SendHelpInfoFlow,
        private val slackManager: SlackManager
) {

    @PostMapping(
            value = ["/interactive"],
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
            produces = [MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseStatus(HttpStatus.OK)
    fun slackInteractiveResponse(
            @RequestParam payload: String,
            @RequestBody body: String,
            @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
            @RequestHeader("X-Slack-Signature") signature: String
    ) {
        slackManager.verifyRequest(body, timestamp, signature)
        val payloadDto: PayloadDto = mapper.readValue(payload)
        val pickedAction = payloadDto.actions.first()
        when {
            pickedAction.value == "next" -> newGuessFlow.newGuess(payloadDto.user.id, payloadDto.team.id)
            pickedAction.value == "help" -> sendHelpInfoFlow.sendHelpInfo(payloadDto.user.id, payloadDto.team.id)
            else -> { // Means we're evaluating a guess.
                val splitParts = pickedAction.value?.split("..") ?: throw IllegalStateException("Missing clicked button value!")
                val correct = splitParts[0]
                val correctName = splitParts[1]
                val position = splitParts[2]
                evaluateGuessFlow.evaluateGuess(correct == "correct", correctName, position, payloadDto.user.id, payloadDto.team.id)
            }
        }
    }
}