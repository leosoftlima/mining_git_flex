class MergeCommitParents

	def initialize(mergeCommitSHA, leftSHA, rightSHA)
		@mergeCommit = mergeCommitSHA
		@left = leftSHA
		@right = rightSHA
	end

	def getMergeCommit()
		@mergeCommit
	end
	
	def getLeft()
		@left
	end
		
	def getRight()
		@right
	end


end