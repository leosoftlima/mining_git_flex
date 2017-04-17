require 'require_all'
require_relative '../lib/MergeCommitParents'

class GitProject

	def initialize(project, localClone, login, password)
			@projetcName = project
			@localClone = localClone
			@path = cloneProjectLocally(project, localClone)
			@mergeScenarios = Array.new
			@mergeCommitParents = Array.new 
			getMergesScenariosByProject()
			@login = login
			@password = password
			puts "merges list length = #{getMergeScenarios().length}" #debugging...
	end

	def getLogin()
		@login
	end

	def getPassword()
		@password
	end

	def getPath()
		@path
	end


	def getLocalClone()
		@localClone
	end

	def getProjectName()
		@projetcName
	end

	def getMergeScenarios()
		@mergeScenarios
	end

	
	def getMergeCommitParents()
		@mergeCommitParents
	end

	def cloneProjectLocally(project, localClone)
		Dir.chdir localClone
		require 'fileutils'
		folder = getProjectName.gsub("/", "_")
		folder_path = File.join(localClone, folder)
		unless Dir.exist?(folder_path)
			FileUtils::mkdir_p File.join(localClone, folder)
		end
		if Dir["#{folder_path}/*"].empty?
			%x(git clone https://github.com/#{project}.git #{folder})
		end
		Dir.chdir folder
		Dir.pwd
	end

	def deleteProject()
		Dir.chdir getLocalClone()
		delete = %x(rd /s /q localProject)
	end

	def getMergesScenariosByProject()
		Dir.chdir getPath()
		@mergeScenarios = Array.new 
		@mergeCommitParents = Array.new # meu
		merges = %x(git log --pretty=format:'%H' --merges)
		merges.each_line do |mergeScenario|
			@mergeScenarios.push(mergeScenario.gsub("\n","").gsub("\'",""))
			parents = getParentsMerge(mergeScenario.gsub("\n","").gsub("\'",""))
			#puts "merge commit SHA = #{mergeScenario.gsub("\n","").gsub("\'","").dump}" #debugging...
			#puts "parent left SHA = #{parents[0].gsub("\n","").gsub("\'","").dump}" #debugging...
			#puts "parent Right SHA = #{parents[1].gsub("\n","").gsub("\'","").dump}" #debugging...
			mergeCommitParentsObj = MergeCommitParents.new(mergeScenario.gsub("\n","").gsub("\'",""), parents[0], parents[1])						
			@mergeCommitParents.push(mergeCommitParentsObj)
			puts "Merge: #{mergeCommitParentsObj.getMergeCommit()}; Left: #{mergeCommitParentsObj.getLeft()}; Right: #{mergeCommitParentsObj.getRight()}"
		end
	end

	
	def getParentsMerge(commit)
	    parentsCommit = Array.new
		commitParent = %x(git cat-file -p #{commit})
		commitParent.each_line do |line|
			if(line.include?('author'))
				break
			end
			if(line.include?('parent'))
				commitSHA = line.partition('parent ').last.gsub('\n','').gsub(' ','').gsub('\r','')
				parentsCommit.push(commitSHA[0..39].to_s)
			end
		end

		if (parentsCommit.size > 1)
			return parentsCommit
		else
			return nil
		end
	end
	
	def generateCommitsListFromLeftRight(projectName, mergeCommitParents, pathResults)
		dataList = []
		mergeCommitParents.each do |mergeParents|
				#puts "Merge: #{mergeParents.getMergeCommit()};Left:#{mergeParents.getLeft()}; Right:#{mergeParents.getRight()}" #debugging...
				mergeCommitID = mergeParents.getMergeCommit()
				left = mergeParents.getLeft()
				right = mergeParents.getRight()
				
				ancestor = %x(git merge-base #{left} #{right}).gsub("\r","").gsub("\n","")
				countLeftCommits = %x(git rev-list --count #{ancestor}..#{left} ).gsub("\r","").gsub("\n","")
				listLeftCommits = %x(git rev-list #{ancestor}..#{left} ).gsub("\n","@@")
				countRightCommits = %x(git rev-list --count #{ancestor}..#{right}).gsub("\r","").gsub("\n","")
				listRightCommits = %x(git rev-list #{ancestor}..#{right}).gsub("\n","@@")
				if !countLeftCommits.eql? "0" and  !countRightCommits.eql? "0"				
					data = mergeCommitID+","+left+","+right+","+ancestor+","+listLeftCommits+","+listRightCommits
					dataList.push(data.gsub("\n", ""))				
				end
		end

		Dir.chdir File.expand_path("..", Dir.pwd)
		csvFile1 = File.expand_path("..", Dir.pwd)
		csvFile2 = File.join(pathResults,projectName.gsub("/", "_"))+"_merges.csv"
		csvFile = File.join(csvFile1,csvFile2)

		 File.open(csvFile, 'w') do |file|
			file.puts "https://github.com/#{projectName}"
			file.puts "MERGE, LEFT, RIGHT, BASE, LEFT_COMMITS, RIGHT_COMMITS"
			dataList.each do |dataMerge|
				file.puts "#{dataMerge}"
			end
		 end
	end
			
	def getNumberMergeScenarios()
		return @mergeScenarios.length
	end



end