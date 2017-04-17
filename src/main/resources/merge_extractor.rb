require 'rubygems'
require 'lib/MainAnalysisProjects'

def extract_merges(inputFile, login, password, result, repositories)
  projectsList = []
  File.open(inputFile, "r") do |text|
    indexLine = 0
    text.each_line do |line|
      projectsList[indexLine] = line[/\"(.*?)\"/, 1]
      indexLine += 1
    end
  end

  project = MainAnalysisProjects.new(login, password, result,
                                     repositories, projectsList)
  project.runAnalysis()
end