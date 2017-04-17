require 'require_all'
require_relative '../lib/GitProject'
require_relative '../lib/MergeCommitParents'

class MainAnalysisProjects

	def initialize(loginUser, passwordUser, pathResults, pathRepositories, projectsList)
		@loginUser = loginUser
		@passwordUser = passwordUser
		@pathResults = pathResults
		Dir.chdir pathRepositories
		@localClone = Dir.pwd
		@projectsList = projectsList
	end

	def getLocalCLone()
		@localClone
	end

	def getLoginUser()
		@loginUser
	end

	def getPasswordUser()
		@passwordUser
	end

	def getPathResults()
		@pathResults
	end

	def getProjectsList()
		@projectsList
	end

	def printStartAnalysis()
		puts "*************************************"
		puts "-------------------------------------"
		puts "####### START COMMITS SEARCH #######"
		puts "-------------------------------------"
		puts "*************************************"
	end

	def printProjectInformation (index, project)
		puts "Project [#{index}]: #{project}"
	end

	def printFinishAnalysis()
		puts "*************************************"
		puts "-------------------------------------"
		puts "####### FINISH COMMITS SEARCH #######"
		puts "-------------------------------------"
		puts "*************************************"
	end

	def runAnalysis()
		printStartAnalysis
		index = 1

		@projectsList.each do |project|
			printProjectInformation(index, project)
			mainGitProject = GitProject.new(project, getLocalCLone(), getLoginUser(), getPasswordUser())
			projectName = mainGitProject.getProjectName()
			#puts "projectName = #{projectName}"		#debugging...
			mergeList = mainGitProject.getMergeScenarios
			#puts "getMergeScenarios = #{mergeList}"		#debugging...
			mergeCommitParents = mainGitProject.getMergeCommitParents
			#puts "getMergeCommitParents = #{mergeCommitParents.length}"		#debugging...
			mainGitProject.generateCommitsListFromLeftRight(projectName, mergeCommitParents, getPathResults)
			index += 1
			if mergeList.length == 0
				mainGitProject.deleteProject
			end
		end

		printFinishAnalysis
	end

end



