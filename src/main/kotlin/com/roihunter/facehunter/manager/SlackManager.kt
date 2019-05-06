package com.roihunter.facehunter.manager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.roihunter.facehunter.dto.slack.channel.OpenChannelDto
import com.roihunter.facehunter.dto.slack.user.AllUsersDto
import com.roihunter.facehunter.dto.slack.user.UserDto
import khttp.get
import khttp.post
import org.springframework.stereotype.Service

@Service
class SlackManager(
        private val mapper: ObjectMapper
) {

    fun postSlackMessage(userId: String, botToken: String) {
        val channelId = getImChannelIdForUserId(userId, botToken)
        val response = post(
                url = "https://slack.com/api/chat.postMessage",
                params = mapOf(
                        "channel" to channelId,
                        "text" to "Guess!",
                        "token" to botToken
                )
        )
    }

    /**
     * Retrieves all users in given Slack workspace.
     */
    fun getAllUsersInWorkspace(botToken: String): List<UserDto> {
        val allUsersResponse = get(
                url = "https://slack.com/api/users.list",
                params = mapOf("token" to botToken)
        )
        val allUsersDto: AllUsersDto = mapper.readValue(allUsersResponse.text)
        return allUsersDto.members
    }

    /**
     * Retrieves IM channel based on the user id.
     */
    private fun getImChannelIdForUserId(userId: String, botToken: String): String {
        val imOpenResponse = post(
                url = "https://slack.com/api/im.open",
                params = mapOf(
                        "token" to botToken,
                        "user" to userId
                )
        )
        val imChannelDto: OpenChannelDto = mapper.readValue(imOpenResponse.text)
        return imChannelDto.channel.id
    }
}